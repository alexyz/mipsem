/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import cfern.fs.*;
import cfern.sys.str.*;

/**
 * Methods for <sys/stat.h>
 */
public class StatInterface extends Interface {
  
  public StatInterface(SystemInterface sys) {
    super(sys);
  }
  
  /**
   * Change access mode of file
   */
  public int chmod(int path_p, int mode) {
    String path = mem.load_string(path_p);
    opt.warn("chmod (%s, %o) ignored", path, mode);
    // in future call rootfs.chmod(path, mode, mask) ...
    // even if thats just a stub
    return 0;
  }
  
  /**
   * File status
   */
  public int fstat(int fd, int stat_p) {
    return fstat(fd, stat_p, false);
  }
  
  /**
   * File status 64
   */
  public int fstat64(int fd, int stat64_p) {
    return fstat(fd, stat64_p, true);
  }
  
  /**
   * File status implementation. Writes to struct (is64 ? stat64 : stat). 
   */
  private int fstat(int fd, int stat_p, boolean is64) {
    FileDesc file = files.getfd(fd);
    opt.info("fstat %s (fd %d: %s, %s)", is64 ? "64" : "", fd, file, mem.getname(stat_p));
    if (file == null)
      return con.error(ebadf);
    if (!mem.bound(stat_p))
      return con.error(efault);
    
    file.fstat().store(mem, stat_p, is64);
    return 0;
  }
  
  /**
   * File status
   */
  public int stat (int path_p, int stat_p) {
    return stat(path_p, stat_p, false, false);
  }
  
  /**
   * File status 64
   */
  public int stat64 (int path_p, int stat_p) {
    return stat(path_p, stat_p, true, false);
  }
  
  /**
   * Link status.
   */
  public int lstat(int path_p, int stat_p) {
    opt.info("lstat: stub calling stat");
    return stat(path_p, stat_p, false, true);
  }
  
  /**
   * Link status 64.
   */
  public int lstat64(int path_p, int stat_p) {
    opt.info("lstat64: stub calling stat64");
    return stat(path_p, stat_p, true, true);
  }
  
  /**
   * File status of unopened file implementation
   */
  private int stat (int path_p, int stat_p, boolean is64, boolean link) {
    String path = mem.load_string(path_p);
    opt.info("stat%s(%s,%s)", is64 ? "64" : "", path, mem.getname(stat_p));
    if (path == null || !mem.bound(stat_p))
      return con.error(efault);
    if (path.length() == 0) {
      opt.warn("stat: could not find %s", path);
      return con.error(enoent);
    }
    
    Stat[] st = new Stat[1];
    String ret = FileSystem.get().stat(st, files.abspath(path), link);
    if (ret != null)
      return con.error(ret);
    
    opt.info("stat: got stat %s", st[0]);
    st[0].store(mem, stat_p, is64);
    return 0;
  }
  
  /**
   * Make directory
   */
  public int mkdir(int path_p, int mode) {
    String path = mem.load_string(path_p);
    opt.info("mkdir(%s,%o)", path, mode);
    if (path == null)
      return con.error(efault);
    if (path.length() == 0)
      return con.error(enoent);
    
    String err = FileSystem.get().mkdir(files.abspath(path), mode, files.getumask());
    return con.error(err);
  }
  
  /**
   * Set user file creation mask.
   */
  public int umask(int m) {
    int oldm = files.getumask();
    opt.info("umask(%o) old=%o", m, oldm);
    files.setumask(m);
    return oldm;
  }
  
  /**
   * Stat filing system for given path.
   */
  public int statfs(int path_p, int buf_p) {
    return statfs(path_p, buf_p, false);
  }
  
  /**
   * Stat filing system 64 bit for given path.
   * long sys_statfs64(const char __user *path, size_t sz, struct statfs64 __user *buf).
   * See linux/fs/open.c
   */
  public int statfs64(int path_p, int sz, int buf_p) {
    if (sz != con.get("sizeof_statfs64"))
      return con.error(einvalid);
    return statfs(path_p, buf_p, true);
  }
  
  /**
   * Stat filing system for given path implementation.
   * int statfs(const char *path, struct statfs *buf);
   */
  private int statfs(int path_p, int buf_p, boolean is64) {
    String path = mem.load_string(path_p);
    opt.info("statfs%s(%s,%s)", is64 ? "64" : "", mem.getname(path_p), mem.getname(buf_p));
    if (path == null || !mem.bound(buf_p))
      return con.error(efault);
    
    StatFS[] ret = new StatFS[1];
    String err = FileSystem.get().statfs(ret, path);
    if (err != null)
      return con.error(err);
    ret[0].store(mem, buf_p, is64);
    return 0;
  }
  
  /**
   * Stat filing system for open file.
   */
  public int fstatfs(int fd, int buf_p) {
    return fstatfs(fd, buf_p, false);
  }
  
  /**
   * Stat filing system for open file.
   * TODO check parameters
   */
  public int fstatfs64(int fd, int sz, int buf_p) {
    if (sz != con.get("sizeof_statfs64"))
      return con.error(einvalid);
    return fstatfs(fd, buf_p, true);
  }
  
  /**
   * Stat filing system for open file implementation.
   * int fstatfs(int fd, struct statfs *buf);
   */
  private int fstatfs(int fd, int buf_p, boolean is64) {
    FileDesc file = files.getfd(fd);
    opt.info("fstatfs%s(%s,%s)", is64 ? "64" : "", file, mem.getname(buf_p));
    if (file == null)
      return con.error(ebadf);
    if (!mem.bound(buf_p))
      return con.error(efault);
    
    StatFS st = file.fstatfs();
    st.store(mem, buf_p, is64);
    return 0;
  }
  
}























