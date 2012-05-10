/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;

import java.io.IOException;

/**
 * Factory for creating file systems
 */
public interface Factory {

  /**
   * Create a new instance of this file system
   */
  FileSystem newInstance(String mount, String dev) throws IOException;
  
  /**
   * Get short name of this file system
   */
  String name();
}
