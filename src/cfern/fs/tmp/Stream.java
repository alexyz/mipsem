/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.io.*;
import cfern.io.*;

/**
 * A virtual file backed by streams created whenever the file is opened.
 * Anonymous subclasses typically override in, out, in and out or tty. Use
 * FixedStream if in and out are constant.
 */
public abstract class Stream extends Device {
  
  /**
   * Constructor for subclasses
   */
  protected Stream(int major, int minor) {
    super(major, minor);
  }
  
  /**
   * Returns this
   */
  public final Stream stream() {
    return this;
  }

  /**
   * Return the output stream. Subclasses may create these when the file is
   * opened. By default returns tty.out() if not null, or sink.
   */
  protected OutputStream out() {
    return OutputStreamUtil.sink;
  }
  
  /**
   * Return the input stream, possibly creating. By default returns tty.in() if
   * not null, otherwise eof.
   */
  protected InputStream in() {
    return InputStreamUtil.eof;
  }
  
  /**
   * FIXME experimental
   */
  protected Stream open() {
    return this;
  }
  
}
