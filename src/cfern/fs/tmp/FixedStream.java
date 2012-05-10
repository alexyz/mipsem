/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.io.*;

/**
 * A stream with constant input and output.
 */
public class FixedStream extends Stream {

  private final InputStream in;
  private final OutputStream out;
  
  /**
   * Create a stream file with the given streams
   */
  public FixedStream(int major, int minor, InputStream in, OutputStream out) {
    super(major, minor);
    if (in == null || out == null)
      throw new RuntimeException("streams must be non null");
    this.in = in;
    this.out =  out;
  }

  protected OutputStream out() {
    return out;
  }
  
  protected InputStream in() {
    // may be closed
    // should just return -1 on reads
    return in;
  }
  
}
