/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.*;

import cfern.Driver;

/**
 * ...
 */
public class StringChannel {
  
  private final Pipe pipe;
  private final CharsetEncoder enc;
  private final StringBuilder sb = new StringBuilder(128);
  
  public StringChannel() {
    enc = Driver.charset.newEncoder();
    try {
      pipe = Pipe.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public ReadableByteChannel source() {
    return pipe.source();
  }
  
  /**
   * Encode chars into the superclass buffer
   */
  public synchronized void flush() {
    if (sb.length() > 0) {
      CharBuffer cbuf = CharBuffer.wrap(sb);
      try {
        pipe.sink().write(enc.encode(cbuf));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      sb.setLength(0);
    }
  }
  
  /**
   * Insert string into buffer given number of positions back (i.e. 0 means at
   * end). Returns false if nothing was changed.
   */
  public synchronized boolean insert (int back, String s) {
    // should use CharSequence instead of string but need the indexOf method
    int start = sb.length() - back;
    if (start < 0 || (s.indexOf('\n') >= 0 && back > 0))
      return false;
    
    sb.insert(start, s);
    if (sb.indexOf("\n") >= 0)
      flush();
    return true;
  }
  
  /**
   * Remove string from buffer given number of positions back. Returns false if
   * nothing was changed.
   */
  public synchronized boolean remove (int back, int len) {
    int start = sb.length() - back;
    if (start < 0)
      return false;
    
    sb.delete(start, start+len);
    return true;
  }
  
}
