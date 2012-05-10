/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import cfern.sys.str.Stat;

/**
 * A device
 */
public class Device extends File {
  
  private final int rdev;
  
  /**
   * Constructor for subclasses
   */
  public Device(int major, int minor) {
    this.rdev = major * 256 + minor;
  }

  final Stat stat() {
    Stat st = stat(Stat.chr);
    st.rdev = this.rdev;
    st.blksize = 1024;
    st.blocks = 0;
    st.size = 0;
    return st;
  }
  
}
