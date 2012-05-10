/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp.dev;
import java.io.InputStream;
import java.util.*;

import cfern.fs.*;
import cfern.io.*;
import cfern.fs.tmp.*;

/**
 * Access to emulator state
 * TODO needs window, gc, pages, clear, free
 */
public class ProcFileSystem extends TempFileSystem {
  
  private final static String name = "proc";
  
  /**
   * Factory for this file system
   */
  public static final Factory procfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) {
      return new ProcFileSystem(mount);
    }
    public String name() {
      return name;
    }
  };
  
  /**
   * Creates a proc file system.
   * TODO set tempfs read only
   */
  protected ProcFileSystem(String mount) {
    super(mount, null);
    Dir syskernel = root.getdir("sys/kernel", true);
    // in future need to say read only stream
    // also should have type of regular file
    syskernel.put("osrelease", new FixedStream(0, 0, InputStreamUtil.streamFor("2.6.18-4-686\n"), OutputStreamUtil.sink));
    
    root.put("filesystems", new Stream(0, 0) {
      protected InputStream in() {
        Iterator<String> i = VirtualFileSystem.filesystems().iterator();
        return InputStreamUtil.streamFor(i);
      }
    });
    
    root.put("mounts", new Stream(0, 0) {
      protected InputStream in() {
        Iterator<String> i = FileSystem.get().mounts().iterator();
        return InputStreamUtil.streamFor(i);
      }
    });
    
  }
  
  public String getShortName() {
    return name;
  }
  
}
