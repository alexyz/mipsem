/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;
import java.util.Iterator;

import cfern.Driver;

/**
 * Various static input streams and methods.
 */
public final class InputStreamUtil {
  
  /**
   * Unclosable System.in, converts CR/CRLF to LF
   */
  public static final InputStream cin = new ConsoleInputStream(System.in, true);

  /**
   * Uncloseable System.in, no CR conversion
   */
  public static final InputStream cinraw = new ConsoleInputStream(System.in, false);
  
  /**
   * Always returns -1
   */
  public static final InputStream eof = new InputStream() {
    public int read() {
      return -1;
    }
  };

  /**
   * Always throws exception
   */
  public static final InputStream exn = new InputStream() {
    public int read() throws IOException {
      throw new IOException("yawn");
    }
  };

  /**
   * Always returns zeros
   */
  public static final InputStream zeros = new InputStream() {
    public int read() {
      return 0;
    }
  };

  /**
   * Returns random bytes
   */
  public static final InputStream rand = new InputStream() {
    public int read() {
      return (int) (Math.random() * 256);
    }
  };
  
  /**
   * Returns stream for list of strings seperated by line feeds.
   */
  public static InputStream streamFor(Iterator<String> i) {
    StringBuilder sb = new StringBuilder();
    while (i.hasNext())
      sb.append(i.next()).append("\n");
    return streamFor(sb.toString());
  }
  
  /**
   * Return an input stream for given string in system charset.
   * Consider using StringReg instead
   */
  public static InputStream streamFor(String s) {
    return new ByteArrayInputStream(s.getBytes(Driver.charset));
  }
  
  private InputStreamUtil() {
    // private to prevent javadoc
  }

}
