/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import cfern.fs.StreamFileDesc;
import cfern.sys.str.*;

/**
 * A stream file descriptor backed by a temporary file system stream. This adds
 * fstat and terminal capability.
 */
public class TempStreamFD extends StreamFileDesc {
  
  /**
   * The backing stream
   */
  private final Stream stream;
  
  /** 
   * Create a new filer for the given InputStream. Can be used anywhere 
   */
  public TempStreamFD(Stream stream) {
    super(stream.in(), stream.out());
    this.stream = stream;
  }
  
  public Stat fstat() {
    return stream.stat();
  }
  
  public StatFS fstatfs() {
    return stream.getfs().fstatfs();
  }
  
  public String toString() {
    return String.format("%s[%s]", super.toString(), stream);
  }
  
} // end of class TempStreamFD
