/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;

/**
 * An abstract regular unix file descriptor.
 * Extend this with an implementation for a particular filing system.
 */
public abstract class RegFileDesc extends FileDesc {
  
  /**
   * Regular files can always be read from
   */
  public final boolean readable() { 
    return true; 
  }
  
  /**
   * Should always be able to seek regular files
   */
  public final boolean seekable() { 
    return true;
  }
  
  /**
   * Yes we are a regular file
   */
  public final boolean isreg() {
    return true;
  }
  
  /**
   * Must implement this for regular files
   */
  protected abstract int seek(int pos, Seek whence);
  
} // end of class UnixReg
