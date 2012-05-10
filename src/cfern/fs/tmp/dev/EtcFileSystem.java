/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp.dev;
import cfern.fs.*;
import cfern.fs.tmp.*;

/**
 * Initialise various configuration files
 */
public class EtcFileSystem extends TempFileSystem {
  
  private final static String name = "etc";
  
  /**
   * Factory for this file system
   */
  public static final Factory etcfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) {
      return new EtcFileSystem(mount);
    }
    public String name() {
      return name;
    }
  };
  
  /**
   * Creates a proc file system.
   */
  protected EtcFileSystem(String mount) {
    super(mount, null);
    
    root.put("passwd", new StringReg("root:x:0:0:root:/:/bin/sh\n"));
    root.put("group", new StringReg("root:x:0:\n"));
    root.put("profile", new StringReg("echo Cfern\n"));
    root.put("issue", new StringReg("Cfern!!\n"));
    root.put("issue.net", new StringReg("Cfern!!\n"));
    root.put("utf8_日本語", new StringReg("utf8 test: 日本語"));
    
  }
  
  public String getShortName() {
    return name;
  }
  
}
