/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.nat;
import java.io.*;
import cfern.fs.*;
import cfern.sys.str.*;

/**
 * A unix directory file descriptor backed by a native File
 */
class NativeDirFD extends DirFileDesc {
  
  private final NativeFileSystem fs;
  
  /**
   * The directory
   */
  private final File dir;
  
  /**
   * Create a unix file that represents a native directory
   */
  public NativeDirFD(NativeFileSystem fs, File dir) {
    if (!dir.isDirectory())
      throw new RuntimeException("not a dir: " + dir);
    this.fs = fs;
    this.dir = dir;
  }
  
  /**
   * List all filenames in directory
   */
  public DirEnt[] list () {
    File[] files = dir.listFiles();
    DirEnt[] ents = new DirEnt[files.length];
    for (int n = 0; n < files.length; n++)
      ents[n] = new DirEnt(files[n], n);
    return ents;
  }
  
  public Stat fstat() {
    return fs.fstat(dir);
  }
  
  public StatFS fstatfs() {
    return fs.fstatfs();
  }
  
} // end of class NativeDir
