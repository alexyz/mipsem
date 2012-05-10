/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import cfern.fs.*;
import cfern.sys.Terminal;
import cfern.sys.str.*;

/**
 * A stream file descriptor backed by a temporary file system stream. This adds
 * fstat and terminal capability.
 */
public class TempChannelFD extends ChannelFileDesc {
  
  /**
   * The backing stream
   */
  private final Channel channel;
  
  /** 
   * Create a new filer for the given InputStream. Can be used anywhere 
   */
  public TempChannelFD(Channel channel) {
    super(channel.inChannel(), channel.outChannel());
    this.channel = channel;
  }
  
  public Stat fstat() {
    return channel.stat();
  }
  
  public StatFS fstatfs() {
    return channel.getfs().fstatfs();
  }
  
  public Terminal getTerminal() {
    return channel.getTerminal();
  }
  
  public String toString() {
    return String.format("%s[%s]", super.toString(), channel);
  }
  
} // end of class TempStreamFD
