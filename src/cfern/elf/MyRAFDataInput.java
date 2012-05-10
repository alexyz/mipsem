/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;

import java.io.IOException;
import java.io.RandomAccessFile;

import cfern.fs.MyDataInput;
import cfern.fs.FileDesc;

/**
 * A RandomAccessFile backing of MyDataInput, for the ELF loader. This is so we
 * don't have to run cfern itself and mount file systems just to dump an ELF
 * file. This class should not be used anywhere else.
 */

class MyRAFDataInput implements MyDataInput {
  /**
   * The backing file
   */
  private final RandomAccessFile raf;
  
  /**
   * Create a MyDataInput backed by a RandomAccessFile
   */
  public MyRAFDataInput(RandomAccessFile raf) {
    this.raf = raf;
  }
  
  public void close() {
    try {
      raf.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public byte read() throws IOException {
    return raf.readByte();
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    return raf.read(buf, off, len);
  }
  
  public int readint() throws IOException {
    return raf.readInt();
  }
  
  public short readshort() throws IOException {
    return raf.readShort();
  }
  
  public int seekset(int pos) throws IOException {
    raf.seek(pos);
    return 0;
  }
  
  public int seekcur(int len) throws IOException {
    raf.skipBytes(len);
    return 0;
  }
  
  /**
   * Returns null
   */
  public FileDesc getfile() {
    throw new UnsupportedOperationException();
  }
}
