/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;

import cfern.Driver;

/**
 * Prevents closing of input stream, also converts carriage returns to line
 * feeds
 */
class ConsoleInputStream extends FilterInputStream {
  
  private final boolean win, mac;
  
  ConsoleInputStream(InputStream in, boolean removecr) {
    super(in);
    boolean win = false, mac = false;
    if (removecr) {
      String sep = System.getProperty("line.separator");
      if (sep.equals("\r"))
        mac = true;
      else if (sep.equals("\r\n"))
        win = true;
    }
    this.win = win;
    this.mac = mac;
  }

  public void close() {
    Driver.opt().info("ignored attempt to close console input");
  }

  public int read() throws IOException {
    int b = read();
    return b != '\r' ? b : (mac ? '\n' : (win ? read() : b));
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    int r = in.read(buf, off, len);
    //System.err.println("read: " + Driver.toString(buf, off, off + r));
    if (r > 0) {
      if (win) {
        int cr = 0;
        // scan for cr, shift left by number found
        for (int i = off, j = off; j < (off + r); i++, j++) {
          if (buf[j] == '\r') {
            cr++;
            j++;
          }
          if (j > i)
            buf[i] = buf[j];
        }
        r -= cr;
        
      } else if (mac) {
        // just replace
        for (int i = off; i < (off + r); i++) {
          if (buf[i] == '\r')
            buf[i] = '\n';
        }
      }
    }
    //System.err.println("returning: " + Driver.toString(buf, off, off + r));
    return r;
  }

}
