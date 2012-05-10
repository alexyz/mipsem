/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.io.*;
import cfern.fs.*;
import cfern.sys.str.*;

/**
 * A temp filing system regular unix file descriptor.
 */
class TempRegFD extends RegFileDesc {

  private final Reg file;
  private final boolean writeable;
  private int pos = 0;
  
  TempRegFD(Reg rf, boolean writeable, boolean trunc) {
    this.file = rf;
    this.writeable = writeable;
    if (writeable && trunc)
      rf.truncate(0);
  }
  
  protected void closeImp() {
    // yawn
  }
  
  public int getsize() {
    return file.getsize();
  }
  
  public Stat fstat() {
    return file.stat();
  }
  
  public StatFS fstatfs() {
    return file.statfs();
  }
  
  public byte read() throws IOException {
    int b = file.get(pos);
    if (b < 0)
      throw new EOFException("end of virtual file");
    pos++;
    return (byte) b;
  }
  
  public int read(byte[] buf, int off, int len) {
    for (int n = 0; n < len; n++) {
      int b = file.get(pos);
      if (b < 0)
        return n;
      buf[off + n] = (byte) b;
      pos++;
    }
    return len;
  }
  
  public void write(byte b) throws IOException {
    if (!writeable)
      throw new IOException("virtual file not writeable");
    
    if (file.put(pos, b))
      pos++;
    else
      throw new IOException("virtual file overflow");
  }
  
  public boolean writeable() {
    return writeable;
  }
  
  public int available() {
    int len = file.getsize();
    return (len > pos) ? (len - pos) : -1;
  }
  
  protected int seek(int pos, Seek whence) {
    if (whence == Seek.SET)
      this.pos = pos;
    else if (whence == Seek.CUR)
      this.pos += pos;
    else if (whence == Seek.END)
      this.pos = file.getsize() + pos;
    
    return pos;
  }
  
} // end of class VirtualReg
