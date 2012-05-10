/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;

/**
 * An abstract regular file. See subclasses SparseReg and StringReg.
 */
public abstract class Reg extends File {
  
  /**
   * Returns this
   */
  public Reg file() {
    return this;
  }
  
  abstract void open(boolean truncate);
  
  /**
   * Return current file length.
   */
  abstract int getsize();
  
  /**
   * Put byte at offset. Returns false on overflow.
   */
  abstract boolean put(int off, byte b);
  
  /**
   * Get byte at offset.
   * Returns -1 at end of file.
   * Returns 0 for empty regions.
   */
  abstract int get(int off);
  
  /**
   * Set length of file
   */
  abstract void truncate(int newsize);

}
