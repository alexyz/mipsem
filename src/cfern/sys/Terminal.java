/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.nio.channels.*;
import cfern.sys.str.Termios;

/**
 * The terminal.
 * Not quite sure what this should do...
 * TODO width, height, pause, get/set session, group
 */
public class Terminal {
  
  private final int num;
  private final String name;
  private final ReadableByteChannel in;
  private final WritableByteChannel out;
  private final Termios ios;
  
  /**
   * Create terminal
   */
  public Terminal(ReadableByteChannel in, WritableByteChannel out, String name, int num) {
    this.in = in;
    this.out = out;
    this.name = name;
    this.num = num;
    this.ios = new Termios().init();
  }
  
  public String getname() {
    return name;
  }
  
  /**
   * Get the options for this terminal. Never returns null.
   */
  public Termios getTermios() {
    return ios;
  }
  
  /**
   * Get the pts number of this terminal.
   */
  public int num() {
    return num;
  }
  
  /**
   * Returns terminal name.
   */
  public String toString() {
    return String.format("Terminal[%s]", name);
  }

  
  public ReadableByteChannel in() {
    return in;
  }

  public WritableByteChannel out() {
    return out;
  }
}
