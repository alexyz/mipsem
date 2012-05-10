/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;

/**
 * Various static output streams
 */
public final class OutputStreamUtil {
  
  /**
   * Uncloseable System.out.
   * Note the hosted program uses System.out exclusively and the emulator uses System.err exclusively.
   */
  public static final OutputStream cout = new ConsoleOutputStream(System.out);
  
  /**
   * Discards all bytes
   */
  public static final OutputStream sink = new OutputStream() {
    public void write(int b) {
      // discard
    }
  };

  /**
   * Always throws exception
   */
  public static final OutputStream exn = new OutputStream() {
    public void write(int b) throws IOException {
      throw new IOException("example write exception");
    }
  };
  
  private OutputStreamUtil() {
    // private to prevent javadoc
  }

}
