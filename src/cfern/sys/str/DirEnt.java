/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;
import java.io.File;

import cfern.mem.Memory;

/**
 * A class representing a C struct dirent or dirent64.
 */
public class DirEnt extends Struct {
  
  // types in linux/include/linux/fs.h L1011
  // 4 is dir, 8 is regular file
  public static final int DIR = 4, FILE = 8;

  public int inode;
  public int offset;
  public String name;
  public int type;
  /**
   * Length of record, is set by store() as length may be dependent on character
   * encoding of name
   */
  public int reclen;
  
  public DirEnt() {
    //
  }
  
  /**
   * Initialise dirent from native file
   */
  public DirEnt(File f, int n) {
    String name = f.getName();
    int h = name.hashCode();
    this.inode = h = (h ^ (h >>> 16)) & 0xffff;
    this.name = name;
    this.offset = n;
    this.type = f.isDirectory() ? DirEnt.DIR : DirEnt.FILE;
  }
  
  /**
   * Store dirent to memory
   */
  public void store(Memory mem, int dirent_p, boolean is64) {
    String dirent = is64 ? "dirent64" : "dirent";
    store_field(mem, dirent, "d_ino", dirent_p, inode);
    store_field(mem, dirent, "d_off", dirent_p, offset);
    reclen = store_field(mem, dirent, "d_name", dirent_p, name);
    store_field(mem, dirent, "d_reclen", dirent_p, reclen);
    if (is64)
      store_field(mem, dirent, "d_type", dirent_p, type);
  }
}
