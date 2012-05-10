/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import cfern.fs.*;
import cfern.sys.str.*;

/**
 * A temp file system directory file descriptor. 
 */
class TempDirFD extends DirFileDesc {
  
  private final Dir dir;
  
  TempDirFD(Dir dir) {
    this.dir = dir;
  }

  protected DirEnt[] list() {
    return dir.list();
  }
  
  public Stat fstat() {
    return dir.stat();
  }
  
  public StatFS fstatfs() {
    return dir.getfs().fstatfs();
  }
  
}
