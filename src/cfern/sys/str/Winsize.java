/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;
import cfern.mem.Memory;

/**
 * A class representing a C struct winsize.
 */
public class Winsize extends Struct {
  
  public int row;
  public int col;
  public int xpixel;
  public int ypixel;
  
  public Winsize init() {
    row = 24;
    col = 80;
    xpixel = 0;
    ypixel = 0;
    return this;
  }
  
  public Winsize load(Memory mem, int arg_p) {
    row = load_field(mem, "winsize", "ws_row", arg_p);
    col = load_field(mem, "winsize", "ws_col", arg_p);
    xpixel = load_field(mem, "winsize", "ws_xpixel", arg_p);
    ypixel = load_field(mem, "winsize", "ws_ypixel", arg_p);
    return this;
  }
  
  public Winsize store(Memory mem, int arg_p) {
    store_field(mem, "winsize", "ws_row", arg_p, row);
    store_field(mem, "winsize", "ws_col", arg_p, col);
    store_field(mem, "winsize", "ws_xpixel", arg_p, xpixel);
    store_field(mem, "winsize", "ws_xpixel", arg_p, ypixel);
    return this;
  }
  
  public String toString() {
    return String.format("Winsize[row=%d col=%d x=%d y=%d]", row, col, xpixel, ypixel);
  }

}
