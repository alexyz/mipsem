/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;

/**
 * A unix input/output stream file descriptor backed by an InputStream or
 * OutputStream.
 */
public abstract class StreamFileDesc extends FileDesc {
  
  /**
   * The OutputStream
   */
  private final OutputStream out;
  
  /**
   * The InputStream
   */
  private final InputStream in;
  
  /** 
   * Create a new file descriptor for the given streams. Can be used anywhere 
   */
  public StreamFileDesc(InputStream in, OutputStream out) {
    if (in == null || out == null)
      throw new IllegalArgumentException("must specify in and out");
    this.in = in;
    this.out = out;
  }
  
  /**
   * Can read
   */
  public boolean readable() { 
    return in != null;
  }
  
  /**
   * Can write
   */
  public boolean writeable() { 
    return out != null; 
  }
  
  /**
   * Cannot seek streams
   * @deprecated
   */
  public boolean seekable() { 
    return false;
  }
  
  public int available() {
    try {
      // stream may be closed, available only returns 0 though
      return in.available();
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }
  
  /**
   * Streams are character devices (though pipes are not)
   */
  public boolean ischr() {
    return true;
  }
  
  /**
   * Is the next read likely to block.
   * Use to back Unix read() calls, may not be necessary
   * @deprecated
   */
  public boolean willblock() {
    try {
      if (in != null)
        return in.available() == 0;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
  
  /**
   * Actually close the streams
   */
  public void closeImp() throws IOException {
    if (out != null)
      out.flush();
    if (in != null) 
      in.close();
    if (out != null) 
      out.close();
  }
  
  /**
   * Write a sequence of bytes to the backing stream
   */
  public void write(byte[] b, int off, int len) throws IOException {
    if (out == null)
      throw new RuntimeException("stream not writeable");
    //System.err.println("<" + new String(b, off, len) + ">");
    out.write(b, off, len);
    out.flush();
  }
  
  /**
   * Write a single byte.
   * TODO this method does not seem to work when called from fast string methods in Memory
   */
  public void write(byte b) throws IOException {
    if (out == null)
      throw new RuntimeException("stream not writeable");
    out.write(b & 0xff);
  }
  
  /**
   * Reads from the backing InputStream into the given array.
   */
  public int read(byte[] buf, int off, int len) throws IOException {
    if (in == null)
      throw new RuntimeException("stream not readable");
    // CRLF conversion done in ConsoleInputStream
    return in.read(buf, off, len);
  }
  
  public byte read() {
    // elf load from stream?
    throw new RuntimeException("implement this");
  }

  public String toString() {
    if (getTerminal() != null)
      return super.toString();

    return String.format("%s[%s,%s]", super.toString(),
        in.getClass().getSimpleName(), out.getClass().getSimpleName());
  }
  
} // end of class UnixStream
