/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.IOException;
import java.util.*;

import cfern.Driver;
import cfern.sys.str.*;

/**
 * The top level file system with methods that delegate to another instance of
 * FileSystem with the appropriate mount point (this filesystem is itself not
 * mounted).
 * FIXME path methods (i.e. all of them) may involve cross fs links...
 */
public class VirtualFileSystem extends FileSystem {
  
  /**
   * Map of file system names to constructors (args are always String mount, String dev).
   */
  private static final Map<String, Factory> fses = Collections.synchronizedMap(new TreeMap<String, Factory>());
  
  static {
    addfs(cfern.fs.nat.NativeFileSystem.natfac);
    addfs(cfern.fs.tmp.TempFileSystem.tmpfac);
    addfs(cfern.fs.tmp.dev.DeviceFileSystem.devfac);
    addfs(cfern.fs.tmp.dev.EtcFileSystem.etcfac);
    addfs(cfern.fs.tmp.dev.ProcFileSystem.procfac);
    addfs(cfern.fs.tmp.dev.NativeZipFileSystem.zipfac);
    addfs(cfern.fs.tmp.dev.VirtualZipFileSystem.vzipfac);
  }
  
  /**
   * Add factory
   */
  private static void addfs(Factory fac) {
    fses.put(fac.name(), fac);
  }
  
  /**
   * Return list of possible file systems
   */
  public static Set<String> filesystems() {
    return fses.keySet();
  }
  
  /**
   * List of loaded filesystems.
   */
  private final Vector<FileSystem> mounts = new Vector<FileSystem>();

  /**
   * Create a new top level file system
   */
  VirtualFileSystem() {
    super(null, null);
  }
  
  public String getShortName() {
    return "vfs";
  }
  
  /**
   * Returns mounted filesystems in same format as linux /proc/mounts
   */
  public List<String> mounts() {
    // /dev/hda3 / ext3 rw,data=ordered 0 0
    List<String> ret = new ArrayList<String>(mounts.size());
    for (FileSystem fs : mounts)
      ret.add(String.format("%s %s %s %s 0 0", fs.getShortName(), fs.getmount(), fs.getShortName(), fs.getopt()));
    return ret;
  }
  
  /**
   * Mount a file system with given options at given mount point.
   * Returns false if unable to mount.
   */
  public boolean mount(String type, String dev, String at) {
    String mount = at.endsWith("/") ? at : at.concat("/");
    
    try {
      Factory f = fses.get(type.toLowerCase());
      if (f == null) {
        Driver.opt().error("mount: no such file system %s", type);
        return false;
      }
      
      // not sure if this is actually an error in linux
      for (FileSystem fs : mounts) {
        if (fs.getmount().startsWith(at)) {
          Driver.opt().error("mount: cannot mount as %s is equal or below %s", fs, at);
          return false;
        }
      }
      
      FileSystem fs = f.newInstance(mount, dev);
      mounts.add(fs);
      Driver.opt().info("mounted %s", fs);
      return true;
      
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  /**
   * Open file/dir with given options (read, write, create, exclusive, append,
   * truncate) by delegating to a file system implementation. Returns either a
   * string error code or null with the file in uf[0].
   */
  public String open(FileDesc[] uf, String path, boolean rd, boolean wr, boolean cr, boolean ex, boolean ap, boolean tr) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.open(uf, fs.relpath(path), rd, wr, cr, ex, ap, tr);
  }
  
  /**
   * Stat the given file. Returns string error code or null with stat result in
   * st[0].
   */
  public String stat(Stat[] st, String path, boolean link) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.stat(st, fs.relpath(path), link);
  }
  
  /**
   * Create a directory
   */
  public String mkdir(String path, int mode, int mask) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.mkdir(fs.relpath(path), mode, mask);
  }
  
  /**
   * Check file access
   */
  public String access(String path, int mode) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.access(fs.relpath(path), mode);
  }
  
  /**
   * Remove a file
   */
  public String unlink(String path) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.unlink(fs.relpath(path));
  }
  
  /**
   * Remove a file
   */
  public String rmdir(String path) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.unlink(fs.relpath(path));
  }
  
  /**
   * Stat fs
   */
  public String statfs(StatFS[] ret, String path) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.statfs(ret, fs.relpath(path));
  }
  
  /**
   * Rename a file on the same filing system
   */
  public String rename(String from, String to) {
    from = FileSystemUtil.reduce(from);
    to = FileSystemUtil.reduce(to);
    FileSystem fs = fsfor(from), fsto = fsfor(to);
    if (fs != fsto)
      return exdev;
    
    Driver.opt().info("vfs: renaming %s to %s for %s", from, to, fs);
    return fs.rename(fs.relpath(from), fs.relpath(to));
  }
  
  /**
   * Rename a file on the same filing system
   */
  public String readlink(String[] targetret, String path) {
    path = FileSystemUtil.reduce(path);
    FileSystem fs = fsfor(path);
    return fs.readlink(targetret, fs.relpath(path));
  }
  
  /**
   * Create symlink
   */
  public String symlink(String to, String from) {
    from = FileSystemUtil.reduce(from);
    FileSystem fs = fsfor(from);
    return fs.symlink(to, fs.relpath(from));
  }
  
  /**
   * Get the filing system for a given absolute (and reduced) path. Never
   * returns null (returns fs at / if nothing else). If path is a mounted dir,
   * the mounted fs must be returned, not the containing fs.
   */
  private FileSystem fsfor(String apath) {
    if (apath.length() == 0)
        throw new RuntimeException("blank path");
    if (apath.equals(".") || apath.equals(".."))
      throw new RuntimeException("file path is relative");
    
    // find longest matching mount
    int fsn = -1, len = 0;
    for (int n = 0; n < mounts.size(); n++) {
      FileSystem fs = mounts.get(n);
      String mount = fs.getmount();
      // /c/x -> /c/, /c -> /c/, /cf -> /
      if (apath.startsWith(mount) || (mount.startsWith(apath) && (apath.length() == mount.length() - 1))) {
        if (mount.length() > len) {
          fsn = n;
          len = mount.length();
        }
      }
    }
    
    if (fsn == -1)
      throw new RuntimeException("could not find /");
    
    FileSystem ret = mounts.get(fsn);
    Driver.opt().fslog("fsfor: %s -> %s", apath, ret);
    return ret;
  }
  
  /**
   * Return shortest unix path for this native path by searching all mounted
   * filesystems. Returns null if not visible from any mount.
   */
  public String unixPathFor(String npath) {
    String ret = null;
    for (int n = 0; n < mounts.size(); n++) {
      FileSystem fs = mounts.get(n);
      String upath = fs.unixPathFor(npath);
      if (upath != null && (ret == null || upath.length() < ret.length()))
        ret = upath;
    }
    if (ret == null)
      Driver.opt().error("FileSystems: could not get unix path of %s");
    return ret;
  }
  
} // end of class VirtualFileSystem
