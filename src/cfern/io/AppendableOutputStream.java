/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;
import cfern.Driver;

/**
 * Sends calls to write to the appendable.
 */
public class AppendableOutputStream extends OutputStream {

  /**
   * Write text to here
   */
  private final Appendable app;

  /**
   * Buffer until newlines
   */
  private final StringBuilder sb = new StringBuilder(8192);

  /**
   * Create output stream for appendable. Only app.append(CharSequence) will be
   * called. It will always be synchronized.
   */
  public AppendableOutputStream(Appendable app) {
    this.app = app; 
  }

  /** 
   * Write byte to Appendable
   */
  public void write(int b) {
    write(new byte[] { (byte) b }, 0, 1);
  }

  /** 
   * Write bytes to Appendable
   */
  public void write(byte[] buf, int pos, int len) {
    //cfern.cpu.Machine.opt().info("JTextAreaOutputStream.write(byte[%d],%d,%d)", buf.length, pos, len);
    if (len == 0)
      return;

    // TODO this is pretty inefficient
    String s = new String(buf, pos, len, Driver.charset);
    if (s.length() == 0)
      return;

    synchronized (this) {
      if (s.indexOf('\n') == -1) {
        //cfern.cpu.Machine.opt().println("JTextAreaOutputStream: buffering %d chars", s.length());
        sb.append(s);
      } else {
        if (sb.length() == 0) {
          flush(s);
        } else {
          sb.append(s);
          flush();
        }
      }
    }
  }

  public synchronized void flush() {
    if (sb.length() > 0) {
      // toString allocates a string with a new backing array
      flush(sb.toString());
      sb.setLength(0);
    }
  }

  /**
   * Write the String to the Appendable.
   * Must be synchronized.
   */
  private void flush(final String s) {
    if (s.length() == 0)
      return;
    
    //Machine.opt().println("JTextAreaOutputStream: flushing %d chars, nl=%d", s.length(), s.indexOf('\n'));
    
    try {
      app.append(s);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


} // end of class TextAreaPrinter
