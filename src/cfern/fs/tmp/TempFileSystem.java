/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.util.Arrays;

import cfern.Driver;
import cfern.fs.*;
import cfern.sys.Constants;
import cfern.sys.str.*;

/**
 * A virtual filing system. Structure and file content is kept in memory only,
 * however subclasses may back the file content with something else (like a zip
 * file).
 * TODO needs unlink and ro option
 * perhaps dev and zip should be in subpackage
 */
public class TempFileSystem extends FileSystem {
  
  private final static String name = "tmp";
  
  /**
   * Factory for this file system
   */
  public static final Factory tmpfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) {
      return new TempFileSystem(mount, null);
    }
    public String name() {
      return name;
    }
  };

  /**
   * The root directory. Subclasses may add files directly.
   */
  protected final Dir root = new Dir();

  /**
   * Create a new empty virtual file system. Opt is ignored (used by some
   * subclasses).
   */
  protected TempFileSystem(String mount, String opt) {
    super(mount, opt);
    root.setfs(this);
  }
  
  public String getShortName() {
    return name;
  }

  public final String open(FileDesc[] uf, String path, 
      boolean rd, boolean wr, boolean cr, boolean ex, boolean ap, boolean tr) {
    Dir pathd;
    String name;
    File f;
    
    if (path.equals("/")) {
      // just return root directory
      pathd = root;
      f = root;
      name = null;
      
    } else {
      // follow directory path and get file/dir
      String[] patha = FileSystemUtil.split(path);
      // FIXME path may include cross fs link
      pathd = root.getdirfor(patha, false);
      if (pathd == null) {
        Driver.opt().fslog("Temp: could not get dir for %s", Arrays.toString(patha));
        // should be enotdir? depends
        return enoent;
      }
      name = patha[patha.length - 1];
      f = pathd.get(name);
    }
    
    if (f != null && cr && ex)
      // file shouldn't exist
      return eexists;
    
    if (f == null) {
      if (!cr)
        // file not found
        return enoent;
      
      // create new file
      f = new SparseReg();
      pathd.put(name, f);
    }
    
    Reg r = f.file();
    Dir d = f.dir();
    Stream s = f.stream();
    Link l = f.link();
    ChannelOpt c = f.channel();
    FileDesc ret;
    
    if (d != null) {
      if (wr)
        // can't open dirs for writing
        return eisdir;
      ret = new TempDirFD(d);
      
    } else if (r != null) {
      // open, truncate
      r.open(tr);
      if (tr)
        r.truncate(0);
      ret = new TempRegFD(r, wr, tr);
      // append to end of file
      if (ap)
        ret.seekend(0);
      
    } else if (s != null) {
      // should check if stream is writable
      ret = new TempStreamFD(s.open());
      
    } else if (c != null) {
      ret = new TempChannelFD(c.open());
      
    } else if (l != null) {
      // FIXME need a link count, but also for stat
      // also, what if O_CREAT and link target doesn't exist?
      
      String tgt = l.target(getmount(), path);
      return FileSystem.get().open(uf, tgt, rd, wr, cr, ex, ap, tr);
      
    } else {
      throw new RuntimeException("unknown type: " + f);
    }

    uf[0] = ret;
    return null;
  }
  
  public String stat(Stat[] st, String path, boolean link) {
    File f = root.getfile(path);
    if (f == null) {
      //Machine.opt().warn("TempFileSystem: could not find '%s'", path);
      return enoent;
    }
    
    // follow link if not doing lstat
    if (!link) {
      Link l = f.link();
      if (l != null) {
        String tgt = l.target(getmount(), path);
        return FileSystem.get().stat(st, tgt, link);
      }
    }
    
    st[0] = f.stat();
    return null;
  }
  
  public String mkdir(String upath, int mode, int mask) {
    Dir d = root.getdir(upath, true);
    if (d == null)
      return eexists;
    return null;
  }
  
  /**
   * Check file access. Mode is ignored for now.
   */
  public String access(String path, int mode) {
    File f = root.getfile(path);
    return f == null ? enoent : null;
  }
  
  /**
   * Rename/move a file within a filesystem
   * TODO probably should be a method on Dir
   */
  public String rename(String from, String to) {
    // get from dir
    String[] fp = FileSystemUtil.split(from);
    String ff = fp[fp.length - 1];
    Dir fd = root.getdirfor(fp, false);
    if (fd == null)
      return enoent;
    
    // get to dir
    String[] tp = FileSystemUtil.split(to);
    String tf = tp[tp.length - 1];
    Dir td = root.getdirfor(tp, false);
    if (td == null)
      return enoent;
    
    File target = fd.remove(ff);
    if (target == null)
      return enoent;
    
    // overwrite target
    td.remove(tf);
    td.put(tf, target);
    
    return null;
  }
  
  /**
   * Get the link target.
   */
  public String readlink(String[] targetret, String path) {
    File f = root.getfile(path);
    if (f == null)
      return enoent;
    Link l = f.link();
    if (l != null) {
      targetret[0] = l.target();
      return null;
    }
    return einvalid;
  }
  
  /**
   * Create symlink
   */
  public String symlink(String to, String from) {
    String[] froma = FileSystemUtil.split(from);
    String name = froma[froma.length - 1];
    Dir d = root.getdirfor(froma, false);
    if (d == null)
      return enoent;
    File f = d.get(name);
    if (f != null)
      return eexists;
    d.put(name, new Link(to));
    return null;
  }
  
  public String unlink(String path) {
    String[] patha = FileSystemUtil.split(path);
    String name = patha[patha.length - 1];
    Dir d = root.getdirfor(patha, false);
    if (d == null)
      return enoent;
    File f = d.get(name);
    if (f == null)
      return enoent;
    if (f.dir() != null)
      return eisdir;
    d.remove(name);
    return null;
  }
  
  public String statfs(StatFS[] ret, String path) {
    File f = root.getfile(path);
    if (f == null)
      return enoent;
    ret[0] = fstatfs();
    return null;
  }
  
  public StatFS fstatfs() {
    // keyn test $ ./a.out myfile
    // statfs=0 (Success)
    // type=ef53 bs=4096 b=2168283 bf=1088671 ba=978526
    // tf=1103232 ff=938283 nl=255
    Runtime r = Runtime.getRuntime();
    StatFS st = new StatFS();
    st.bavail = (int) (r.freeMemory() / 1024);
    st.bfree = st.bavail;
    st.blocks = (int) (r.totalMemory() / 1024);
    st.bsize = 1024;
    st.files = File.getInodes();
    st.ffree = 1024 - st.files;
    st.fsid = 0;
    st.namelen = 255;
    // subclasses might want to overwrite this
    st.type = Constants.get().get("EXT2_SUPER_MAGIC");
    return st;
  }

}
