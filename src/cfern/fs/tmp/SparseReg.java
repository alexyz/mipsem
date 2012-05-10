/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.io.*;
import java.util.Vector;

import cfern.Driver;
import cfern.mem.Pages;
import cfern.sys.str.Stat;

/**
 * A virtual sparse regular file backed by a list of byte arrays. For initial
 * content, subclass and implement content(). There is no file pointer, it must
 * be provided by VirtualReg. This class is thread safe.
 */
public class SparseReg extends Reg {
  
  /**
   * 4mb max size
   */
  private static final int maxlen = 0x400000;
  
  /**
   * Backing vector of byte arrays.
   */
  private final Vector<byte[]> pages = new Vector<byte[]>();
  private boolean opened = false;
  private int length = 0;
  
  /**
   * Create file for given temp filing system
   */
  public SparseReg() {
    //
  }
  
  /**
   * Initialise the content of the file from the content() method if not
   * truncating.
   */
  synchronized void open(boolean truncate) {
    // sync may not be neccessary
    if (opened)
      return;
    opened = true;
    if (truncate)
      return;

    try {
      InputStream is = content();
      if (is != null) {
        int p = 0, b = 0;
        while ((b = is.read()) >= 0)
          put(p++, (byte) b);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Return current file length.
   */
  int getsize() {
    return length;
  }
  
  /**
   * Put byte at offset. Returns false on overflow.
   */
  synchronized boolean put(int off, byte b) {
    //Driver.opt().println("Reg.put(%d,%d) len=%d", off, b, len);
    if (off < 0 || off >= maxlen)
      return false;

    int p = off / Pages.size;
    // add more null pages
    if (p >= pages.size())
      pages.setSize(p + 1);

    // get page
    byte[] page = pages.get(p);
    if (page == null)
      pages.set(p, page = Pages.alloc());

    page[off % Pages.size] = b;

    if (off >= length)
      length = off + 1;

    return true;
  }
  
  /**
   * Get byte at offset.
   * Returns -1 at end of file.
   * Returns 0 for empty regions.
   */
  synchronized int get(int off) {
    if (off < 0 || off >= length)
      return -1;

    int p = off / Pages.size;
    byte[] page = pages.get(p);
    if (page == null)
      return 0;

    return page[off % Pages.size] & 0xff;
  }
  
  /**
   * Set length of file
   */
  synchronized void truncate(int newsize) {
    if (newsize < 0 || length <= newsize) {
      Driver.opt().debug("Reg: ignored truncate of file length %d to %d", length, newsize);
      return;
    }

    // free the truncated pages
    int startp = (newsize / Pages.size) + ((newsize % Pages.size) == 0 ? 0 : 1);
    int endp = length / Pages.size;
    Driver.opt().warn("Reg: truncate %d to %d: dropping pages %d to %d", length, newsize, startp, endp); 
    while (endp >= startp) {
      Pages.free(pages.get(startp));
      pages.set(startp, null);
      startp++;
    }
    length = newsize;
  }

  /**
   * Initial content. Default implementation returns null.
   */
  protected InputStream content() throws IOException {
    // maybe there could be two methods, this and a boolean to say whether the
    // file should be reinitialised whenever it is opened
    return null;
  }
  
  Stat stat() {
    Stat st = stat(Stat.reg);
    st.blksize = Pages.size;
    st.blocks = pages.size();
    st.size = getsize();
    return st;
  }

}
