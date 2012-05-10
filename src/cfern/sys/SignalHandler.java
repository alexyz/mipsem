/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.util.*;
import cfern.Driver;
import cfern.sys.Constants;
import cfern.sys.str.SigAction;

/**
 * Keeps track of signal handlers for a process. 
 * Also queues and blocks signals (queue is the only thread safe method).
 * Only cares about the first 32 signals.
 * See linux/include/asm-mips/signal.h
 */
public class SignalHandler {
  
  /**
   * Max valid signal plus 1. Note 0 is invalid.
   * In Linux, signals 32 onward are real time signals.
   */
  private static final int numsig = 32;
  
  /**
   * Queued signals (i.e. blocked or waiting for machine to notice it's been
   * interrupted).
   */
  private final ArrayList<SigAction> pending = new ArrayList<SigAction>();
  
  /**
   * Map of signals to actions for this process.
   * Contents never change.
   * Index 0 is null.
   */
  private final SigAction[] acts = new SigAction[numsig];
  
  /**
   * Unblockable signals (kill and stop)
   */
  private final int umask;
  
  /**
   * Creates a new array of signal handlers, all set to SIG_DFL.
   */
  SignalHandler() {
    Constants con = Constants.get();
    
    // prevent blocking of kill and stop
    int killbit = 1 << (con.get("SIGKILL") - 1);
    int stopbit = 1 << (con.get("SIGSTOP") - 1);
    umask = ~(killbit | stopbit);
    
    for (int n = 1; n < acts.length; n++) {
      String name = con.namerx("SIG[A-Z]", n);
      int def = con.geta(name)[1];
      SigAction sa = new SigAction(name, n, def);
      acts[n] = sa;
    }
  }
  
  /**
   * Creates a copy of all signals.
   * Does not copy the queue.
   */
  SignalHandler(SignalHandler other) {
    this.umask = other.umask;
    for (int n = 1; n < acts.length; n++)
      acts[n] = other.acts[n].copy();
  }
  
  /**
   * Get sigaction for specified signal. 
   * Returns null if signal number is invalid.
   */
  SigAction getact(int sig) {
    if (sig <= 0 || sig >= numsig) {
      Driver.opt().error("Signals: %d is invalid", sig);
      return null;
    }
    return acts[sig];
  }
  
  /**
   * The signal mask. A bit set to 1 means blocked (bit 0 is signal 1).
   */
  private int mask = 0;
  
  /**
   * Set the process mask.
   * Automatically prevents blocking of stop or kill.
   */
  public void setmask(int mask) {
    Driver.opt().siglog("mask was %08x now %08x", this.mask, mask);
    this.mask = mask & umask;
  }
  
  /**
   * Get the process mask.
   */
  public int getmask() {
    return mask;
  }
  
  /**
   * Queue this signal. May be called from any thread.
   * Returns true if signal is not blocked (i.e. the machine should be interrupted).
   * TODO: needs to not allow both stop and continue signals
   */
  public synchronized boolean queue(int sig) {
    if (sig <= 0 || sig >= numsig) {
      Driver.opt().error("Signals.queue: %d invalid", sig);
      return false;
    }
    SigAction act = acts[sig];
    boolean intr = false;

    if (act.ignore()) {
      Driver.opt().siglog("Ignoring %s", act);
    } else if (pending.contains(act)) {
      Driver.opt().siglog("Already queued: %s", act);
    } else {
      pending.add(act);
      intr = !act.blocked(mask);
      Driver.opt().siglog("Queue now %s", pending);
    }
    return intr;
  }
  
  /**
   * Get the next unblocked signal handler.
   * Returns null if there are no waiting signals.
   */
  public synchronized SigAction take() {
    for (int n = 0; n < pending.size(); n++) {
      SigAction act = pending.get(n);
      if (!act.blocked(mask))
        return pending.remove(n);
    }
    return null;
  }
  
} // end of class Signals
