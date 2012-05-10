/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;
import cfern.fs.FileDesc;

/**
 * An adapter from FileDesc to InputStream. No seeking allowed.
 * Used only for virtual zip file system.
 */
public class FileDescInputStream extends InputStream {

  /**
   * Backing unix file (doesn't have to be regular).
   */
  private final FileDesc uf;

  /**
   * Create input stream from the given unix file.
   */
  public FileDescInputStream(FileDesc uf) throws IOException {
    if (!uf.readable())
      throw new IOException("not readable: " + uf);
    this.uf = uf;
  }

  public int read() throws IOException {
    try {
      return uf.read();
    } catch (EOFException e) {
      e.printStackTrace();
      return -1;
    }

  }

  public int read(byte[] b, int off, int len) throws IOException {
    return uf.read(b, off, len);
  }

  public long skip(long n) throws IOException {
    int pos = uf.offset();
    int newpos = uf.seekcur((int) n);
    return newpos - pos;
  }
  
} // end of class UnixFileInputStream
