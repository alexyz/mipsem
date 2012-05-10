/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;
import cfern.Driver;
import cfern.mem.Memory;
import cfern.sys.Constants;

/**
 * A class representing a C struct stat.
 * See Open Group Base Specifications Issue 6 or stat manpage.
 */
public class Stat extends Struct {
  /**
   * File types: regular, directory, etc.
   * TODO Should probably make this an enum
   */
  public static final String reg = "S_IFREG", dir = "S_IFDIR", fifo = "S_IFIFO", chr = "S_IFCHR",
      blk = "S_IFBLK", lnk = "S_IFLNK", sock = "S_IFSOCK";
  /**
   * User read, write, execute
   */
  private static final String uread = "S_IRUSR", uwrite = "S_IWUSR", uexecute = "S_IXUSR";
  /**
   * ID of device of filesystem
   */
  public int dev = 1;
  /**
   * Inode, must be non zero. Inode + dev are unique identifier for file.
   */
  public int inode = 1;
  /**
   * Number of hard links to this file (only 1)
   */
  public int nlink = 1;
  /**
   * User id of file
   */
  public int uid = 0;
  /**
   * Group id of file
   */
  public int gid = 0;
  /**
   * Device id for special files
   */
  public int rdev = 0; // should be 1?
  /**
   * Access permissions and type
   */
  public int mode;
  /**
   * Size of file or symbolic link target
   */
  public int size;
  /**
   * Block size of filing system
   */
  public int blksize;
  /**
   * Number of blocks allocated
   */
  public int blocks;
  /**
   * Last access time
   */
  public int atime = 0;
  /**
   * Last modification time
   */
  public int mtime = 0;
  /**
   * Creation time
   */
  public int ctime = 0;
  
  /**
   * Get the mode constant for the given permissions.
   * Type must be one of reg, dir, etc.
   */
  public static int modefor(boolean rd, boolean wr, boolean ex, String type) {
    Driver.opt().fslog("modefor: %s%s%s+%s", rd ? "r" : "", wr ? "w" : "", ex ? "x" : "", type);
    Constants con = Constants.get();
    int mode = 0;
    if (rd)
      mode |= con.get(uread);
    if (wr)
      mode |= con.get(uwrite);
    if (rd || ex)
      mode |= con.get(uexecute);
    mode |= con.get(type);
    return mode;
  }
  
  /**
   * Write this struct to memory as either a struct stat or struct stat64.
   */
  public void store(Memory mem, int stat_p, boolean is64) {
    String stat = is64 ? "stat64" : "stat";
    store_field(mem, stat, "st_atime", stat_p, atime);
    store_field(mem, stat, "st_blksize", stat_p, blksize);
    store_field(mem, stat, "st_blocks", stat_p, blocks);
    store_field(mem, stat, "st_ctime", stat_p, ctime);
    store_field(mem, stat, "st_dev", stat_p, dev);
    store_field(mem, stat, "st_gid", stat_p, gid);
    store_field(mem, stat, "st_ino", stat_p, inode);
    store_field(mem, stat, "st_mode", stat_p, mode);
    store_field(mem, stat, "st_mtime", stat_p, mtime);
    store_field(mem, stat, "st_nlink", stat_p, nlink);
    store_field(mem, stat, "st_rdev", stat_p, rdev);
    store_field(mem, stat, "st_size", stat_p, size);
    store_field(mem, stat, "st_uid", stat_p, uid);
  }
  
  public String toString() {
    return String.format("Stat[dev=%d inode=%d nlink=%d uid=%d gid=%d rdev=%d mode=%d size=%d blksz=%s blocks=%d atime=%d mtime=%d ctime=%d]",
        dev, inode, nlink, uid, gid, rdev, mode, size, blksize, blocks, atime, mtime, ctime);
  }

}
