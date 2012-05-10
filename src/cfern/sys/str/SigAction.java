/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;

import cfern.mem.Memory;
import cfern.sys.Constants;

/**
 * Represents a C struct sigaction. The struct fields are public.
 */
public class SigAction extends Struct {
  /**
   * Default handlers
   */
  private static final int SIG_DFL = 0, SIG_IGN = 1;
  /**
   * Default actions
   */
  private enum DefAct { terminate, ignore, abort, stop, restart }
  /**
   * Signal number (argument to the handler function)
   */
  private final int num;
  /**
   * Signal name
   */
  private final String name;
  /**
   * Default action, never changes
   */
  private final DefAct def;
  /**
   * Handler. Type is int -> void or, if flags has SA_SIGINFO, (int, siginfo_t *, void *) -> void.
   */
  public int handler_fp;
  /**
   * Signal to block during execution of this signal handler.
   * This is actually 128 bytes in size, though only the first word is used.
   */
  public int mask;
  /**
   * Flags e.g. no signal on child stop
   * TODO make this do something
   */
  public int flags;
  /**
   * Unused, obselete
   */
  public int restorer_fp;

  /**
   * Create a sigaction with given name, number, default action. 
   */
  public SigAction(String name, int num, int defInt) {
    Constants con = Constants.get();
    DefAct def;
    int mask = 0;
    
    if (defInt == 0) {
      def = DefAct.terminate;
    } else if (defInt == 1) {
      def = DefAct.ignore;
    } else if (defInt == 2) {
      def = DefAct.abort;
    } else if (defInt == 3) {
      def = DefAct.stop;
      // for stop only allow the continue action (of which there is only one)
      mask = ~(1 << (con.get("SIGCONT") - 1));
    } else if (defInt == 4) {
      def = DefAct.restart;
      // block everything
      mask = -1;
    } else {
      throw new RuntimeException("unknown default signal action: " + defInt);
    }

    this.name = name;
    this.num = num;
    this.def = def;
    this.handler_fp = con.get("SIG_DFL");
    this.mask = mask; 
  }

  /**
   * Copy constructor
   */
  private SigAction(SigAction other) {
    name = other.name;
    num = other.num;
    def = other.def;
    handler_fp = other.handler_fp;
    mask = other.mask;
    flags = other.flags;
    restorer_fp = other.restorer_fp;
  }
  
  /**
   * Load sigaction from memory
   */
  public void load(Memory mem, int sa_p) {
    handler_fp = load_field(mem, "sigaction", "sa_handler", sa_p);
    mask = load_field(mem, "sigaction", "sa_mask", sa_p, true);
    flags = load_field(mem, "sigaction", "sa_flags", sa_p);
    restorer_fp = load_field(mem, "sigaction", "sa_restorer", sa_p);
  }
  
  /**
   * Store sigaction to memory
   */
  public void store(Memory mem, int sa_p) {
    store_field(mem, "sigaction", "sa_handler", sa_p, handler_fp);
    store_field(mem, "sigaction", "sa_mask", sa_p, mask, true);
    store_field(mem, "sigaction", "sa_flags", sa_p, flags);
    store_field(mem, "sigaction", "sa_restorer", sa_p, restorer_fp);
  }
  
  /**
   * Return true if blocked
   */
  public boolean blocked(int mask) {
    return ((mask >>> (num - 1)) & 1) == 1;
  }
  
  /**
   * Return true if this signal should be ignored
   */
  public boolean ignore() {
    int f = handler_fp;
    return f == SIG_IGN || (f == SIG_DFL && def == DefAct.ignore);
  }
  
  /**
   * Return true if this signal should exit the process
   */
  public boolean exit() {
    int f = handler_fp;
    return f == SIG_DFL && (def == DefAct.abort || def == DefAct.terminate); 
  }
  
  /**
   * Returns true if this signal should stop the process
   */
  public boolean stop() {
    int f = handler_fp;
    return f == SIG_DFL && def == DefAct.stop;
  }
  
  /**
   * Return true if this should continue after a stop
   */
  public boolean cont() {
    int f = handler_fp;
    return f == SIG_DFL && def == DefAct.restart;
  }
  
  /**
   * Get the function to call. Only call this after checking ignore(), exit(),
   * stop() and cont().
   */
  public int handler() {
    int f = handler_fp;
    if (f == SIG_IGN || f == SIG_DFL)
      throw new RuntimeException("not a pointer");
    return f;
  }
  
  /**
   * Get the signal mask
   */
  public int getmask() {
    return mask;
  }
  
  /**
   * Get the signal number (for the argument to signal handler)
   */
  public int getnum() {
    return num;
  }
  
  /**
   * Copy this sigaction for fork.
   */
  public SigAction copy() {
    return new SigAction(this);
  }
  
  /**
   * Return a string describing this sigaction
   */
  public String toString(Memory mem) {
    Constants con = Constants.get();
    String f = con.names("SA_", this.flags);
    String h = mem.getname(handler_fp);
    String r = mem.getname(restorer_fp);
    return String.format("SigAction[%s f:%s h:%s m:%08x r:%s d:%s]", name, f, h, mask, r, def);
  }
  
}
