/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import cfern.Driver;
import cfern.sys.str.Stat;

/**
 * A regular file representing a string
 * TODO make max length greater than 256...
 */
public class StringReg extends Reg {
  
  private static final int len = 256;
  
  private final byte[] bytes = new byte[len];
  
  private int length = 0;
  
  /**
   * Create file for given temp filing system
   */
  public StringReg(String val) {
    setString(val);
  }
  
  /**
   * Set the string content
   */
  public synchronized void setString(String val) {
    if (val.length() == 0) {
      length = 0;
    } else {
      byte[] b = val.getBytes(Driver.charset);
      if (b.length > len)
        throw new RuntimeException("string too long");
      System.arraycopy(b, 0, bytes, 0, b.length);
      length = b.length;
    }
  }
  
  /**
   * Get the string content
   */
  public synchronized String getString() {
    return length == 0 ? "" : new String(bytes, 0, length, Driver.charset);
  }
  
  /**
   * File is opened and optionally truncated
   */
  synchronized void open(boolean truncate) {
    if (truncate)
      length = 0;
  }
  
  /**
   * Return current file length.
   */
  int getsize() {
    return length;
  }
  
  synchronized boolean put(int off, byte b) {
    if (off < 0 || off >= len)
      return false;

    if (off >= length) {
      // zero bytes, maybe spaces instead?
      for (int n = length; n < off; n++)
        bytes[n] = 0;
      length = off + 1;
    }
    bytes[off] = b;

    return true;
  }
  
  synchronized int get(int off) {
    return (off < 0 || off >= length) ? -1 : bytes[off];
  }
  
  synchronized void truncate(int newLength) {
    if (newLength < 0 || length <= newLength || newLength > len) {
      Driver.opt().fslog("StringReg: ignored truncate of file length %d to %d", length, len);
      return;
    }
    length = newLength;
  }
  
  Stat stat() {
    Stat st = stat(Stat.reg);
    st.blksize = len;
    st.blocks = 1;
    st.size = length;
    return st;
  }

}
