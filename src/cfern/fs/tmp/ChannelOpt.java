/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;

public abstract class ChannelOpt extends Device {
  
  public ChannelOpt(int major, int minor) {
    super(major, minor);
  }

  protected abstract Channel open();
  
  public ChannelOpt channel() {
    return this;
  }
  
}
