/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.util.Arrays;
import cfern.Driver;
import cfern.sys.str.DirEnt;

/**
 * An abstract unix directory file descriptor. All the implementation need do is
 * provide the list() method.
 */
public abstract class DirFileDesc extends FileDesc {
  
  /**
   * List of files in directory
   */
  private DirEnt[] dir;
  
  /**
   * Index into dirlist
   */
  private int index = 0;
  
  /**
   * Dirs are not readable I think
   */
  public final boolean readable() { 
    return false; 
  }
  
  /**
   * Dirs are not writeable
   */
  public final boolean writeable() { 
    return false; 
  }
  
  /**
   * Directorys are seekable
   */
  public final boolean seekable() { 
    return true;
  }
  
  /**
   * Yes this is a directory.
   */
  public final boolean isdir() { 
    return true; 
  }
  
  public int available() {
    Driver.opt().warn("DirFileDesc: available is always 0");
    return 0;
  }
  
  /**
   * Seek to position in file or to directory entry.
   */
  protected int seek(int pos, Seek whence) {
    if (dir == null) {
      // not sure if this is allowed or not
      Driver.opt().error("DirFileDesc: seek %d (%d) without list", pos, whence);
      return 0;
    }
    if (whence == Seek.SET)
      listseek(pos);
    else if (whence == Seek.CUR)
      listseek(index + pos);
    else if (whence == Seek.END)
      listseek(dir.length + pos);
    return index;
  }
  
  /**
   * Seek to position in directory.
   */
  private synchronized void listseek (int pos) {
    if (pos >= dir.length)
      throw new RuntimeException("invalid dir seek in " + this);
    index = pos;
  }
  
  /**
   * Get the list of files in this directory from the file system implementation
   */
  protected abstract DirEnt[] list();
  
  /**
   * Get next filename in directory
   */
  public synchronized DirEnt listnext () {
    if (dir == null) {
      dir = list();
      Driver.opt().fslog("UnixDir: %s", Arrays.toString(dir));
      index = 0;
    }
    if (index >= dir.length)
      return null;
    return dir[index++];
  }
  
  /**
   * Get the offset in the current directory (i.e. the offset field in struct
   * dirent). Though Linux fills that with some very large unpredictable values.
   */
  public int offset () {
    return index - 1;
  }
  
  /**
   * Throws a runtime exception
   */
  public final void write(byte[] b, int off, int len) {
      throw new RuntimeException("can't write dirs");
  }
  
  /**
   * Throws a runtime exception
   */
  public final void write(byte b) {
      throw new RuntimeException("can't write dirs");
  }
  
  /**
   * Throws a runtime exception
   */
  public final int read(byte[] buf, int off, int len) {
      throw new RuntimeException("can't read dirs");
  }
  
  /**
   * Throws a runtime exception
   */
  public final byte read() {
    throw new RuntimeException("can't read dirs");
  }
  
  /**
   * Size of directory?
   */
  public final int getsize() {
    return 0;
  }
  
} // end of class UnixDir
