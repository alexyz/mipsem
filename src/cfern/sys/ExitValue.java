/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;

/**
 * Immutable object that encapsulates the exit value of a program
 */
final class ExitValue {
  private final byte ret;
  private final int pid;
  
  /** 
   * Create exit value of process pid. Only called from SystemInterface.exit()
   */
  public ExitValue(int pid, byte ret) {
    this.pid = pid;
    this.ret = ret;
  }
  
  /** get return value of this exited process */
  public byte exit() {
    return ret;
  }
  
  /** get process id of this exited process */
  public int pid() {
    return pid;
  }
  
  public String toString() {
    return "{" + pid + " returns " + ret + "}";
  }
  
} // end of class ExitValue
