/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.nat;
import java.io.*;

import cfern.Driver;
import cfern.fs.*;
import cfern.sys.Constants;
import cfern.sys.str.*;

/**
 * Opens files on hosts native file system
 * TODO could probably use an option for case insensitivity of native root
 * TODO safe mode meaning read only
 */
public class NativeFileSystem extends FileSystem {
  
  private static final String name = "nat";
  
  /**
   * Factory for this file system
   */
  public static final Factory natfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) throws IOException {
      return new NativeFileSystem(mount, dev);
    }
    public String name() {
      return name;
    }
  };
  
  /**
   * The native root directory
   */
  private final File f;
  
  /**
   * The native root directory
   */
  private final String nroot;
  
  /**
   * Native directory separator
   */
  private final char nsep;
  
  /**
   * Create a native file system with given root.
   * Throws exception if native root not found.
   */
  protected NativeFileSystem(String mount, String nroot) throws FileNotFoundException {
    super(mount, nroot);
    File f = new File(nroot);
    if (!f.exists() || !f.isDirectory())
      throw new FileNotFoundException("can't find " + nroot);
    String nseps = System.getProperty("file.separator");
    this.f = f;
    this.nsep = nseps.charAt(0);
    this.nroot = nroot.endsWith(nseps) ? nroot : nroot.concat(nseps);
  }
  
  public String getShortName() {
    return name;
  }
  
  /**
   * Convert an absolute unix path into native path
   */
  private String nativePathFor(String upath) {
    return nroot + upath.replace('/', nsep);
  }
  
  /**
   * Get the unix path (including mount point) for the given native path. Return
   * null if not visible from this root.
   */
  public String unixPathFor(String npath) {
    // TODO need to be case insensitive 
    if (!npath.startsWith(nroot))
      return null;
    String rpath = npath.substring(nroot.length());
    String upath = getmount().concat(rpath.replace(nsep, '/'));
    Driver.opt().fslog("unixpath: " + npath + " -> " + upath);
    return upath;
  }
  
  /**
   * Open a native file/dir represented by unix path upath
   */
  public String open(FileDesc[] fd, String upath, boolean rd, boolean wr, boolean creat, boolean excl, boolean app, boolean trunc) {
    String npath = nativePathFor(upath);
    File f = new File(npath);
    Driver.opt().fslog("opened %s", f);
    if (!f.exists() && !creat) {
      Driver.opt().fslog("NativeFileSystem: could not find %s at %s", upath, npath);
      return enoent;
    }
    
    if (f.isDirectory()) {
      if (wr)
        return eisdir;
      fd[0] = new NativeDirFD(this, f);
      return null;
    }

    // open a real file
    try {
      RandomAccessFile raf;
      if (wr) {
        if (f.exists()) {
          if (creat && excl)
            return eexists;  // file shouldn't exist
        } else if (!creat) {
          return enoent; // file not found
        }
        
        raf = new RandomAccessFile(f, "rw");
        if (trunc)
          raf.setLength(0); // erase file contents
        if (app)
          raf.seek(raf.length()); // append to end of file
      } else {
        // just open it read only
        raf = new RandomAccessFile(f, "r");
      }
      fd[0] = new NativeRegFD(this, f, raf, wr);
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Check file access. Mode is ignored for now.
   */
  public String access(String path, int mode) {
    File f = new File(nativePathFor(path));
    // should really check if writable/readable
    return f.exists() ? null : enoent;
  }
  
  public String stat(Stat[] st, String path, boolean link) {
    File f = new File(nativePathFor(path));
    Driver.opt().fslog("Native: stat of %s", f);
    if (!f.exists())
      return enoent;
    st[0] = fstat(f);
    return null;
  }
  
  /**
   * Stat given file
   */
  protected Stat fstat(File f) {
    Stat st = new Stat();
    int time = (int) (f.lastModified() / 1000);
    st.atime = time;
    st.blksize = 4096;
    st.blocks = (int) (f.length() / 4096);
    st.ctime = time;
    st.dev = getdev();
    st.gid = 0;
    st.inode = f.getName().toLowerCase().hashCode();
    // should probably make this instance method
    st.mode = Stat.modefor(f.canRead(), f.canWrite(), f.canExecute(), f.isDirectory() ? Stat.dir : Stat.reg);
    st.mtime = time;
    st.nlink = 1;
    st.rdev = 0;
    st.size = (int) f.length();
    st.uid = 0;
    return st;
  }
  
  public String statfs(StatFS[] ret, String path) {
    File f = new File(nativePathFor(path));
    if (!f.exists())
      return enoent;
    ret[0] = fstatfs();
    return null;
  }
  
  /**
   * Stat this file system
   */
  protected StatFS fstatfs() {
    // see statfs.c
    StatFS st = new StatFS();
    st.bavail = (int) (f.getFreeSpace() / 1024);
    st.bfree = st.bavail;
    st.blocks = (int) (f.getTotalSpace() / 1024);
    st.bsize = 1024;
    st.files = 90000;
    st.ffree = 100000 - st.files;
    st.fsid = 0;
    st.namelen = 255;
    st.type = Constants.get().get("EXT2_SUPER_MAGIC");
    return st;
  }
  
}
