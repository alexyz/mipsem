/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.nat;
import java.io.*;
import cfern.fs.*;
import cfern.sys.str.*;

/**
 * A regular unix file backed by a native RandomAccessFile
 */
class NativeRegFD extends RegFileDesc {
  
  private final NativeFileSystem fs;
  
  private final File f;
  
  /** 
   * The RandomAccessFile of this file
   */
  private final RandomAccessFile raf;
  
  private final boolean writeable;
  
  /**
   * Create a Filer to represent a RandomAccessFile. Specify its creating File
   * object (for toString), and whether its writeable.
   */
  public NativeRegFD(NativeFileSystem fs, File f, RandomAccessFile raf, boolean writeable) {
    this.fs = fs;
    this.f = f;
    this.raf = raf;
    // a randomaccessfile can't actually tell us if its writable
    this.writeable = writeable;
  }
  
  /**
   * Whether this file is writable.
   */
  public boolean writeable() { 
    return writeable; 
  }
  
  public int available() {
    try {
      int len = (int) raf.length(), pos = (int) raf.getFilePointer();
      return (len > pos) ? (len - pos) : -1;
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }
  
  /**
   * Seek to position in file or to directory entry.
   * TODO throw ioexception?
   */
  protected int seek(int pos, Seek whence) {
    try {
      if (whence == Seek.SET)
        raf.seek(pos);
      else if (whence == Seek.CUR)
        raf.seek(raf.getFilePointer() + pos);
      else if (whence == Seek.END)
        raf.seek(raf.length() + pos);
      return (int) raf.getFilePointer();
    } catch (IOException e) {
      throw new RuntimeException("seek error for " + whence + " on " + raf, e);
    }
  }
  
  /**
   * Get the file pointer of the backing RandomAccessFile.
   */
  public int offset() {
    try {
      return (int) raf.getFilePointer();
    } catch (IOException e) {
      throw new RuntimeException("could not get pos of " + raf);
    }
  }
  
  /**
   * Close this file
   */
  protected void closeImp() throws IOException {
    raf.getChannel().force(true);
    raf.close();
  }
  
  /**
   * Write to the file
   */
  public void write(byte[] b, int off, int len) throws IOException {
    raf.write(b, off, len);
  }
  
  /**
   * Write a single byte.
   * FIXME this method does not seem to work when called from fast string methods in Memory
   */
  public void write(byte b) throws IOException {
    raf.write(b & 0xff);
  }
  
  /**
   * Read from random access file
   * FIXME may throw EOF exception, should really return 0
   */
  public int read(byte[] buf, int off, int len) throws IOException {
    int ret = 0;
    ret = raf.read(buf, off, len);
    return ret;
  }
  
  /**
   * Read byte from random access file
   */
  public byte read() throws IOException {
    return raf.readByte();
  }
  
  /**
   * Read a short from random access file
   */
  public short readshort() throws IOException {
    return raf.readShort();
  }
  
  /**
   * Read int from random access file
   */
  public int readint() throws IOException {
    return raf.readInt();
  }
  
  /**
   * Get the size of this file
   */
  public int getsize() {
    int ret = 0;
    try {
      ret = (int) raf.length();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ret;
  }
  
  public Stat fstat() {
    return fs.fstat(f);
  }
  
  public StatFS fstatfs() {
    return fs.fstatfs();
  }
  
} // end of class Filer
