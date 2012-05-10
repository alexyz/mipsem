/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import cfern.Driver;

/**
 * A unix input/output stream file descriptor backed by selectable channels.
 */
public abstract class ChannelFileDesc extends FileDesc {
  
  private final WritableByteChannel outChan;
  private final ReadableByteChannel inChan;
  
  /** 
   * Create a new file descriptor for the given streams. Can be used anywhere 
   */
  public ChannelFileDesc(ReadableByteChannel inChan, WritableByteChannel outChan) {
    if (!(outChan instanceof SelectableChannel && inChan instanceof SelectableChannel))
      throw new RuntimeException("not a selectable channel");
    this.inChan = inChan;
    this.outChan = outChan;
  }
  
  public boolean readable() { 
    return true;
  }
  
  public boolean writeable() { 
    return true;
  }
  
  public boolean seekable() { 
    return false;
  }
  
  public int available() {
    throw new RuntimeException("no available for channels");
  }
  
  public boolean ischr() {
    return true;
  }
  
  public boolean willblock() {
    throw new RuntimeException("no available for channels");
  }
  
  public void closeImp() throws IOException {
      inChan.close();
      outChan.close();
  }
  
  public final byte read() {
    // will never be called by memory layer
    throw new RuntimeException("no single byte read");
  }
  
  public final int read(byte[] buf, int off, int len) throws IOException {
    //Driver.opt().println("ChannelStream: reading %d bytes...", len);
    int ret = inChan.read(ByteBuffer.wrap(buf, off, len));
    Driver.opt().info("ChannelStream: read %d: %s", ret, Driver.toString(buf, off, off + ret));
    return ret;
  }
  
  public final void write(byte b) {
    // will never be called by memory layer
    throw new RuntimeException("no single byte write");
  }
  
  public final void write(byte[] buf, int off, int len) throws IOException {
    //Driver.opt().println("ChannelStream: writing %d bytes...", len);
    outChan.write(ByteBuffer.wrap(buf, off, len));
    Driver.opt().info("ChannelStream: wrote %s", Driver.toString(buf, off, off + len));
  }
  
  public final SelectableChannel inChannel() {
    return (SelectableChannel) inChan;
  }
  
  public final SelectableChannel outChannel() {
    return (SelectableChannel) outChan;
  }

  public String toString() {
    if (getTerminal() != null)
      return super.toString();

    return String.format("%s[%s,%s]", super.toString(),
        inChan.getClass().getSimpleName(), outChan.getClass().getSimpleName());
  }
  
} // end of class UnixStream
