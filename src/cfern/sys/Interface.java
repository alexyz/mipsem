/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import cfern.*;
import cfern.mem.*;

/**
 * Base class for any system interface implementation
 */
public abstract class Interface implements Errors {
  
  /**
   * Constants for the current C library
   */
  protected final Constants con;
  
  /**
   * Debug output options
   */
  protected final Options opt;
  
  /**
   * Same memory image as Machine
   */
  protected final Memory mem;
  
  /**
   * Keeps track of open files
   */
  protected final Files files;
  
  /**
   * First instance constructor
   */
  public Interface(Memory mem, Options opt) {
    this.mem = mem;
    this.opt = opt;
    this.files = new Files();
    this.con = Constants.get();
  }
  
  /**
   * Copy constructor for SystemInterface
   */
  public Interface(Memory mem, Options opt, SystemInterface other) {
    this.mem = mem;
    this.opt = opt;
    this.files = new Files(other.files);
    this.con = other.con;
  }
  
  /**
   * Copy constructor for subinterfaces
   */
  public Interface(SystemInterface other) {
    this.mem = other.mem;
    this.opt = other.opt;
    this.files = other.files;
    this.con = other.con;
  }
  
}
