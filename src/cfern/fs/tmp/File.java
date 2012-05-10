/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.util.concurrent.atomic.AtomicInteger;
import cfern.sys.str.*;

/**
 * An object representing the content of a virtual stream, file, directory or
 * symlink. Exactly one of dir/file/stream/link methods in this class must
 * return non null.
 */
public abstract class File {
  
  /**
   * Serial number generator
   */
  private static final AtomicInteger inodes = new AtomicInteger();
  
  /**
   * Return number of files made
   */
  static int getInodes() {
    return inodes.get();
  }
  
  /**
   * Serial number
   */
  protected final int inode;
  
  /**
   * Permissions
   */
  private boolean read = true, write = true, execute = true;
  
  /**
   * Creation time
   */
  private int ctime = (int) (System.currentTimeMillis() / 1000);
  
  /**
   * Filing system, e.g. for dev number.
   */
  private TempFileSystem fs;
  
  File() {
    // could refuse if there are too many inodes
    inode = inodes.getAndIncrement();
  }
  
  /**
   * Set the filing system (in Dir.put, or manually when creating top level dirs)
   */
  public void setfs(TempFileSystem fs) {
    this.fs = fs;
  }
  
  /**
   * Get the file system holding this file
   */
  public final TempFileSystem getfs() {
    if (this.fs == null)
      throw new RuntimeException("fs not set yet: " + this);
    return fs;
  }
  
  /**
   * Stat this file (may or may not be open).
   * Subclass must call stat(type) to fill in dev, inode, mode, times.
   * Subclasses should fill in block size, blocks, size, rdev
   */
  abstract Stat stat();
  
  StatFS statfs() {
    return null;
  }
  
  /**
   * Fill in generic stat information for subclass
   */
  protected final Stat stat(String type) {
    Stat st = new Stat();
    // TODO move this to TempFileSystem, or at least method to fill in blocks/blksz
    st.dev = getfs().getdev();
    st.inode = inode;
    st.mode = Stat.modefor(read, write, execute, type);
    st.ctime = ctime;
    st.mtime = ctime;
    st.atime = ctime;
    return st;
  }
  
  /**
   * Returns non null if this file is a Dir.
   */
  public Dir dir() {
    return null;
  }

  /**
   * Returns non null if this file is a regular file.
   */
  public Reg file() {
    return null;
  }

  /**
   * Returns non null if this file is a stream.
   */
  public Stream stream() {
    return null;
  }
  
  /**
   * Returns non null if this file is a symbolic (soft) link.
   */
  public Link link() {
    return null;
  }
  
  /**
   * Returns non null if this file is a channel
   */
  public ChannelOpt channel() {
    return null;
  }

}
