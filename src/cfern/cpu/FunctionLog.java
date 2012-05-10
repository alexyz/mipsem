/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;

import cfern.Driver;
import cfern.mem.Memory;

/**
 * Function call logging, providing backtrace functionality.
 * TODO check the stack pointer
 * Maybe this class should be ditched now the stack backtracer works
 */
public class FunctionLog {
  
  /**
   * Function call address backtrace stack
   */
  private final int[] calls = new int[128], callsp = new int[128];
  
  /** memory symbol table for resolving addresses */
  private final Memory mem;
  
  /** pointer into calls[] */
  private int p = 0;
  
  /**
   * Breakpoint addresses
   */
  private int[] bp;
  
  /**
   * Backtrace buffer
   */
  private final StringBuilder sb = new StringBuilder(256);
  
  /**
   * Set memory so we can resolve addresses to symbols
   */
  public FunctionLog (Memory mem) {
    this.mem = mem;
  }
  
  /**
   * copy other functionlog into this, as part of fork
   */
  public void load(FunctionLog other) {
    System.arraycopy(other.calls, 0, calls, 0, calls.length);
    System.arraycopy(other.callsp, 0, callsp, 0, callsp.length);
    // don't bother copying breakpoints?
    p = other.p;
  }
  
  /**
   * clear this functionlog, as part of execve
   */
  public void clear() {
    bp = null;
    p = 0;
    // don't need to clear calls
  }
  
  /**
   * Set the functions for breakpoints.
   * E.g. f,g+10,401230.. in future
   */
  public void setbreakpoints(String list) {
    String[] f = list.split(",");
    if (f.length == 0)
      return;
    
    int[] bp = new int[f.length];
    for (int n = 0; n < f.length; n++) {
      int a = mem.getaddress(f[n]);
      if (a == 0)
        Driver.opt().error("breakpoint: no address for %s", f[n]);
      bp[n] = a;
    }
    this.bp = bp;
  }
  
  /** 
   * A function call as detected in rundebug.
   * Returns true if we just passed a breakpoint 
   */
  public boolean call (int addr, int sp) {
    if (p < calls.length) {
      calls[p] = addr;
      callsp[p] = sp;
    }
    p++;
    
    if (bp == null)
      return false;
    
    for (int n = 0; n < bp.length; n++)
      if (bp[n] == addr)
        return true;
    return false;
  }
  
  /** 
   * A function return, called from rundebug().
   * Returns true if returning to a breakpoint
   */
  public boolean ret (int sp) {
    if (p <= 0)
      throw new RuntimeException("too many returns in ret()");
    p--;
    
    if (bp == null || p > calls.length)
      return false;
    
    int ra = calls[p-1];
    for (int n = 0; n < bp.length; n++)
      if (bp[n] == ra)
        return true;
    return false;
  }
  
  /**
   * Get the current function
   */
  public String top () {
    return mem.getfunname(calls[p - 1]);
  }
  
  /** 
   * generate a backtrace 
   */
  public String bt () {
    if (p == 0)
      return "<no backtrace>";
    sb.delete(0, sb.length());
    for (int n = 0; n < p; n++)
      sb.append('/').append(mem.getfunname(calls[n]));
    // would be nice to return sb but %s format calls toString anyway
    return sb.toString();
  }
  
} // end of class FunctionLog
