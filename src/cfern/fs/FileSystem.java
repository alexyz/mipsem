/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import cfern.Driver;
import cfern.sys.Errors;
import cfern.sys.str.Stat;
import cfern.sys.str.StatFS;

/**
 * Abstract FileSystem base. For the virtual file system instance, use get().
 * Note: any virtual methods added here will also need to be in
 * VirtualFileSystem before they can be used.
 */
public abstract class FileSystem implements Errors {
  
  /**
   * Provides unique device numbers
   */
  private final static AtomicInteger devno = new AtomicInteger(1);
  
  /**
   * Virtual filing system instance.
   */
  private static final FileSystem root = new VirtualFileSystem();
  
  /**
   * The mount point, must end in /
   */
  private final String mount;
  
  /**
   * Mount parameters
   */
  private final String opt;
  
  /**
   * Device number of file system
   */
  private final int dev;
  
  /**
   * Set the mount point and gets a new device number.
   */
  protected FileSystem(String mount, String opt) {
    this.mount = mount;
    this.opt = opt;
    this.dev = devno.getAndIncrement();
  }
  
  /**
   * Get the virtual filing system instance (NOT the / file system, which you
   * should never really need direct access to).
   */
  public static FileSystem get() {
    // perhaps this should be in FileSystemUtil
    return root;
  }
  
  /**
   * Get the device number of this file system instance
   */
  public int getdev() {
    return dev;
  }
  
  /**
   * Get the options (i.e. device) of this file system. TODO options and device
   * should really be seperate, but then none of the filesystems really have
   * virtual devices anyway
   */
  public String getopt() {
    return opt;
  }
  
  /**
   * Get the mount point (always ends in /)
   */
  protected String getmount() {
    return mount;
  }
  
  /**
   * Returns mounts of this file system, never returns null.
   */
  public List<String> mounts() {
    throw new RuntimeException("no mounts");
  }
  
  /**
   * Get the short name of this file system
   */
  public abstract String getShortName();
  
  /**
   * Get the absolute path for this file system from the given absolute (reduced) path. 
   * E.g. 1: /v -> / for /v
   * E.g. 2: /c/myfile -> /myfile for /c
   * E.g. 3: / -> / for /
   */
  public final String relpath(String abspath) {
    if (!abspath.regionMatches(0, mount, 0, mount.length() - 1))
      throw new RuntimeException(abspath + " not on " + mount);
    String ret;
    if (abspath.length() < mount.length())
      ret = "/";
    else
      ret = abspath.substring(mount.length() - 1);
    Driver.opt().fslog("relpath: %s -> %s", abspath, ret);
    return ret;
  }
  
  /**
   * Mount a file system with given device at given mount point.
   * Returns false if unable to mount.
   * (There is no parameter for options yet).
   * TODO add options and return string
   */
  public boolean mount(String type, String dev, String at) {
    throw new RuntimeException("only vfs can mount");
  }
  
  /**
   * Open absolute path to file/dir read only. Throws exception on error.
   */
  public final FileDesc openex(String path) throws IOException {
    FileDesc[] fd = new FileDesc[1];
    String ret = open(fd, path);
    if (ret != null)
      throw new IOException("openex " + path + ": " + ret);
    return fd[0];
  }
  
  /**
   * Open absolute unix path to file/dir read only. Returns either a string
   * error code or null with the file in uf[0].
   */
  public final String open(FileDesc[] fd, String path) {
    return open(fd, path, true, false, false, false, false, false);
  }
   
  /**
   * Open a file, possibly creating etc. Path must start with /, be mount point
   * independent and non blank. Returns either string error code or null with
   * file in uf[0].
   */
  public abstract String open(FileDesc[] uf, String path, boolean rd, boolean wr, boolean cr, boolean ex, boolean ap, boolean tr);
  
  /**
   * Create directory with path, mode limited by process mask.
   * For root, path must be absolute and non blank.
   * For others, path must be relative.
   * Returns null on success.
   * Base implementation returns eaccess.
   */
  public String mkdir(String path, int mode, int mask) {
    return erofs;
  }
  
  /**
   * Ask the implementation to stat a file.
   * Path must be absolute and non blank.
   * Returns string error code or null with result in st[0].
   */
  public String stat(Stat[] st, String path, boolean link) {
    return eio;
  }
  
  /**
   * Check file access.
   * Path must be absolute and non blank.
   */
  public String access(String path, int mode) {
    return eaccess;
  }
  
  /**
   * Unlink (delete) a file, but not a directory.
   * Path must be absolute and non blank.
   */
  public String unlink(String path) {
    // should return enoent/enotdir if not found
    return erofs;
  }
  
  /**
   * Rename/move a file within a filesystem.
   */
  public String rename(String from, String to) {
    return erofs;
  }
  
  /**
   * Get the link target.
   */
  public String readlink(String[] ret, String path) {
    return einvalid;
  }
  
  /**
   * Create symlink
   */
  public String symlink(String to, String from) {
    return erofs;
  }
  
  /**
   * Remove directory
   */
  public String rmdir(String path) {
    return erofs;
  }
  
  /**
   * Stat fs
   */
  public String statfs(StatFS[] ret, String path) {
    return enosys;
  }
  
  /**
   * Return the unix path for the given native path. Return null if not visible.
   */
  public String unixPathFor(String npath) {
    return null;
  }
  
  /**
   * Return a description of this file system
   */
  public String toString() {
    String name = getClass().getSimpleName();
    if (opt == null)
      return name + " on " + mount;
    else
      return name + "[" + opt + "] on " + mount;
  }
  
} // end of class FileSystem
