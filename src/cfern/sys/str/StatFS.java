/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;

import cfern.mem.Memory;

/**
 * A class representing a C struct statfs.
 * See linux statfs manpage
 */
public class StatFS extends Struct {
  public int type;
  public int bsize;
  public int blocks;
  public int bfree;
  public int bavail;
  public int files;
  public int ffree;
  public long fsid;
  public int namelen;
  public void store(Memory mem, int statfs_p, boolean is64) {
    String struct = is64 ? "statfs64" : "statfs";
    store_field(mem, struct, "f_type", statfs_p, type);
    store_field(mem, struct, "f_bsize", statfs_p, bsize);
    store_field(mem, struct, "f_blocks", statfs_p, blocks);
    store_field(mem, struct, "f_bfree", statfs_p, bfree);
    store_field(mem, struct, "f_bavail", statfs_p, bavail);
    store_field(mem, struct, "f_files", statfs_p, files);
    store_field(mem, struct, "f_ffree", statfs_p, ffree);
    store_field(mem, struct, "f_fsid", statfs_p, fsid);
    store_field(mem, struct, "f_namelen", statfs_p, namelen);
  }
}
