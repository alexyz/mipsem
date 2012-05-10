/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;
import java.util.Arrays;
import cfern.mem.Memory;
import cfern.sys.Constants;

/**
 * A class representing a C struct termios.
 */
public class Termios extends Struct {
  /** Input modes */
  int iflag;
  /** Output modes */
  int oflag;
  /** Control modes */
  int cflag; 
  /** Local modes */
  int lflag;
  /**
   * Control chars. Glibc says NCCS is 32 but linux kernel
   * source/include/asm-mips/termbits.h says it is 23. Crashes ash if values is
   * over 23.
   */
  final byte[] cc = new byte[23]; 
  
  public void load(Memory mem, int stat_p) {
    iflag = load_field(mem, "termios", "c_iflag", stat_p);
    oflag = load_field(mem, "termios", "c_oflag", stat_p);
    cflag = load_field(mem, "termios", "c_cflag", stat_p);
    lflag = load_field(mem, "termios", "c_lflag", stat_p);
    load_field(mem, "termios", "c_cc", stat_p, cc);
  }
  
  public void store(Memory mem, int stat_p) {
    store_field(mem, "termios", "c_iflag", stat_p, iflag);
    store_field(mem, "termios", "c_oflag", stat_p, oflag);
    store_field(mem, "termios", "c_cflag", stat_p, cflag);
    store_field(mem, "termios", "c_lflag", stat_p, lflag);
    store_field(mem, "termios", "c_cc", stat_p, cc, 17);
  }
  
  /**
   * Load with typical values
   */
  public Termios init() {
    // from term.c
    Constants con = Constants.get();
    iflag = con.get("IXON|ICRNL"); // 0x500;
    oflag = con.get("OPOST|ONLCR"); // 0x5;
    cflag = con.get("CREAD|CS8"); // 0xbf;
    // ECHO is default on linux but causes ash to echo everything
    lflag = con.get("ISIG|ICANON|ECHOE|ECHOK"); // 0x8a3b;
    Arrays.fill(cc, (byte) 0);
    cc[con.get("VINTR")] = 3;
    cc[con.get("VQUIT")] = 28;
    cc[con.get("VERASE")] = 127;
    cc[con.get("VKILL")] = 21;
    cc[con.get("VMIN")] = 1;
    cc[con.get("VTIME")] = 0;
    cc[con.get("VEOL2")] = 0;
    cc[con.get("VSWTC")] = 0;
    cc[con.get("VSTART")] = 17;
    cc[con.get("VSTOP")] = 19;
    cc[con.get("VSUSP")] = 26;
    cc[con.get("VREPRINT")] = 18;
    cc[con.get("VDISCARD")] = 15;
    cc[con.get("VWERASE")] = 23;
    cc[con.get("VLNEXT")] = 22;
    cc[con.get("VEOF")] = 4;
    cc[con.get("VEOL")] = 0;
    return this;
  }
  
  public String toString() {
    // doesnt print cc
    return String.format("Termios[if=%x of=%x cf=%x lf=%x]", iflag, oflag, cflag, lflag);
  }

}
