/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import java.nio.channels.SelectableChannel;

import cfern.Driver;
import cfern.sys.*;
import cfern.sys.str.*;

/**
 * An abstract file descriptor for regular files, streams, directories, pipes
 * and sockets. Each of those has a subclass that itself may be
 * extended with a concrete implementation for a particular filing system. To
 * open a file, use FileSystem.get().open(...).
 * 
 * TODO need status flags
 */
// would rather have subclasses only visible to subpackages
public abstract class FileDesc implements MyDataInput, Errors {
  
  /**
   * Misc socket options
   */
  public enum SockOpt {
    reuseAddr, keepAlive
  }
  
  /**
   * Seek modes. Can't use system interface constants because they are not
   * visible here
   */
  public enum Seek {
    SET, CUR, END
  }
  
  /**
   * Number of threads that have this file instance
   */
  private int refCount = 1;
  
  /**
   * Block mode for this file, default is to block on reads
   */
  private boolean nonBlock = false;
  
  /**
   * Whether this file can be read from.
   * @deprecated
   */
  public abstract boolean readable();
  
  /**
   * Whether this file is writable.
   * @deprecated
   */
  public abstract boolean writeable();
  
  /**
   * Only regular files and dirs are seekable
   * @deprecated
   */
  public abstract boolean seekable();
  
  /**
   * Whether this Filer represents a directory.
   * @deprecated
   */
  public boolean isdir() {
    return false;
  }
  
  /**
   * Return true if this is a regular file
   */
  public boolean isreg() {
    return false;
  }
  
  /**
   * Is a symbolic link
   * @deprecated
   */
  public boolean islink() {
    return false;
  }
  
  /**
   * Is a socket.
   * @deprecated
   */
  public boolean issock() {
    return false;
  }
  
  /**
   * Set socket option
   */
  public void setSocketOption(SockOpt so) {
    throw new RuntimeException("not a socket");
  }

  /**
   * Is fifo for fstat
   * @deprecated
   */
  public boolean isfifo() {
    return false;
  }
  
  /**
   * Is a char device for fstat
   * @deprecated
   */
  public boolean ischr() {
    return false;
  }
  
  /**
   * Is a block device for fstat
   * @deprecated
   */
  public boolean isblk() {
    return false;
  }
  
  /**
   * Is this file a symbolic link
   * @deprecated
   */
  public boolean islnk() {
    return false;
  }
  
  /**
   * Get the size of this file.
   * By default returns 0.
   */
  public int getsize() {
    return 0;
  }
  
  /**
   * Get the terminal of this stream, returns null if is not a stream or there
   * is no terminal.
   */
  public Terminal getTerminal() {
    return null;
  }
  
  /**
   * Returns a stat of this file.
   */
  public abstract Stat fstat();
  
  /**
   * Returns a stat of this file's filing system.
   */
  public abstract StatFS fstatfs();
  
  /**
   * Increase the refcount of this file for fork().
   * Returns this FileDesc for convienience.
   */
  public synchronized final FileDesc dup() {
    refCount++;
    return this;
  }
  
  /**
   * Seek relative to the start of a file or directory entry
   */
  public final int seekset(int pos) {
    return seek(pos, Seek.SET);
  }
  
  /**
   * Seek relative to the current position of a file or directory entry
   */
  public final int seekcur(int pos) {
    return seek(pos, Seek.CUR);
  }
  
  /**
   * Seek relative to the end of a file or directory entry
   */
  public final int seekend(int pos) {
    return seek(pos, Seek.END);
  }
  
  /**
   * Seek to position in file or to directory entry.
   */
  protected int seek(int pos, Seek whence) {
    throw new RuntimeException("no seek available");
  }
  
  /**
   * Get position in file, or the index of current directory entry.
   */
  public int offset() {
    return 0;
  }
  
  /**
   * Get the name of the next directory entry. Only available for directories.
   */
  public DirEnt listnext() {
    throw new RuntimeException("no listnext");
  }
  
  /**
   * Is the next read likely to block.
   * Use to back Unix read() calls, may not be necessary
   * @deprecated
   */
  public boolean willblock() {
    return false;
  }
  
  /**
   * Return number of bytes that can be read without block or -1 on end of file.
   * This method never blocks.
   * 
   * Note: sometimes this will return 0 even if data is available. this is the
   * case for master/slave terminal pairs which use channels directly. in that
   * case you should use a selector with inChannel and outChannel. In fact if
   * inChannel returns not null you should not use this.
   */
  public abstract int available();
  
  /**
   * Decrements refcount. If it becomes 0 then the file is really closed using
   * closeImp().
   */
  public final synchronized void close() {
    refCount--;
    
    if (refCount > 0) {
      Driver.opt().info("close: not closing %s (%d references remain)", this, refCount);
      return;
    }
    
    Driver.opt().info("close: going to close %s", this);
    
    // actually close if refcount is 0
    try {
      closeImp();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Request the backing implementation close the streams.
   */
  protected void closeImp() throws IOException {
    //
  }
  
  /**
   * Connect this socket.
   * Returns null or string error code on error.
   */
  public String connect(String host, int port) {
    return enotsock;
  }
  
  /**
   * Bind this server socket to given address
   */
  public String bind(String host, int port) {
    return enotsock;
  }
  
  /**
   * Accept connections to this server socket.
   * Returns null if not a socket.
   */
  public FileDesc accept() {
    return null;
  }
  
  /**
   * Write a single byte.
   */
  public abstract void write(byte b) throws IOException;
  
  /**
   * Write a sequence of bytes to the file. Default implementation calls
   * write(byte) repeatedly.
   */
  public void write(byte[] buf, int off, int len) throws IOException {
    if (!writeable())
      throw new IOException("virtual file not writeable");
    for (int n = 0; n < len; n++)
      write(buf[off + n]);
  }
  
  /**
   * Read a byte (for application use only).
   * Throws EOFException if end of file is reached.
   */
  public abstract byte read() throws IOException;
  
  /**
   * Read bytes into array. Returns number of bytes read or -1 on EOF. Does NOT
   * throw EOFException.
   */
  public abstract int read(byte[] buf, int off, int len) throws IOException;
  
  /**
   * Read a word (for application use only).
   * Throws EOFException if end of file is reached.
   */
  public int readint() throws IOException {
    byte a = read();
    byte b = read();
    byte c = read();
    byte d = read();
    return (((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff));
  }
  
  /**
   * Read short (for application use only).
   * Throws EOFException if end of file is reached.
   */
  public short readshort() throws IOException {
    byte a = read();
    byte b = read();
    return (short) ((a << 8) | (b & 0xff));
  }
  
  /**
   * Returns this
   */
  public FileDesc getfile() {
    return this;
  }
  
  /**
   * Print a useful description of this file
   */
  public String toString () {
    return String.format("%s[%s%s%s#%d]", getClass().getSimpleName(), 
        (readable() ? "r" : ""), (writeable() ? "w" : ""), nonBlock ? "!B" : "", refCount);
  }

  /**
   * Was this file opened or set non blocking?
   */
  public boolean isNonBlock() {
    return nonBlock;
  }

  /**
   * Set the nonblock state of this file
   */
  public synchronized void setNonBlock(boolean nonBlock) {
    // FIXME maybe this should be subclassed by SocketFileDesc?
    this.nonBlock = nonBlock;
  }
  
  public SelectableChannel inChannel() {
    return null;
  }
  
  public SelectableChannel outChannel() {
    return null;
  }
  
} // end of class UnixFile
