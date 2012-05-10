/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.nio.channels.*;
import cfern.sys.Terminal;

public class Channel extends ChannelOpt {
  
  private final ReadableByteChannel inChan;
  private final WritableByteChannel outChan;
  private final Terminal tty;
  
  public Channel(int major, int minor, Terminal tty) {
    this(major, minor, tty.in(), tty.out(), tty);
  }
  
  public Channel(int major, int minor, ReadableByteChannel inChan, WritableByteChannel outChan) {
    this(major, minor, inChan, outChan, null);
  }
  
  public Channel(int major, int minor, ReadableByteChannel inChan, WritableByteChannel outChan, Terminal tty) {
    super(major, minor);
    if (!(outChan instanceof SelectableChannel && inChan instanceof SelectableChannel))
      throw new RuntimeException("not a selectable channel");
    this.inChan = inChan;
    this.outChan = outChan;
    this.tty = tty;
  }

  protected ReadableByteChannel inChannel() {
    return inChan;
  }
  
  protected WritableByteChannel outChannel() {
    return outChan;
  }
  
  protected Terminal getTerminal() {
    return tty;
  }
  
  protected Channel open() {
    return this;
  }
  
}
