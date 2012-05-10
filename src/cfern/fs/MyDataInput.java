/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;

import java.io.IOException;

/**
 * An interface similar to java.io.DataInput, but drops boring methods and adds
 * close and seek methods. For use by the ELF loader.
 */
public interface MyDataInput {
  /**
   * Read an int
   */
  int readint() throws IOException;

  /**
   * Read a short
   */
  short readshort() throws IOException;

  /**
   * Read a byte
   */
  byte read() throws IOException;

  /**
   * Read up to len bytes into array.
   * Returns number of bytes read.
   */
  public int read(byte[] buf, int off, int len) throws IOException;

  /**
   * Seek to position relative to start of file. Ignore the return value.
   */
  int seekset(int pos) throws IOException;

  /**
   * Seek to position relative to current position. Ignore the return value.
   */
  int seekcur(int len) throws IOException;

  /**
   * Close the backing file.
   */
  void close();
  
  /**
   * Get the implementing unix file if any.
   * This is so the elfloader can load using mem.load_from(unixfile) 
   */
  FileDesc getfile();
}
