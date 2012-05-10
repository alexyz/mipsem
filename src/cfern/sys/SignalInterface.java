/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import cfern.cpu.*;
import cfern.sys.str.SigAction;

/**
 * Represents functions in <signal.h>
 */
public class SignalInterface extends Interface {
  
  /**
   * Signals for this process
   */
  private final SignalHandler signals;
  
  public SignalInterface(SystemInterface sys) {
    super(sys);
    signals = new SignalHandler();
  }
  
  public SignalInterface(SystemInterface sys, SignalInterface sig) {
    super(sys);
    signals = new SignalHandler(sig.signals);
  }
  
  /**
   * Get the signals
   */
  public SignalHandler getSignalHandler() {
    return signals;
  }
  
  /**
   * Examine and change a signal action
   */
  public int sigaction(int sig, int act_p, int oldact_p, int sigsetsize) {
    opt.info("sigaction (%s, act %s, oldact %s, sigsetsize %d)",
          con.name("SIG", sig), mem.getname(act_p), mem.getname(oldact_p), sigsetsize);
    
    SigAction sa = signals.getact(sig);
    if (sa == null)
      return con.error(einvalid);
    
    if (oldact_p != 0) {
      // return the old signal
      sa.store(mem, oldact_p);
      opt.siglog("stored old sigaction %s", sa.toString(mem));
    }
    
    if (act_p != 0) {
      if (con.is(sig, "SIGKILL") || con.is(sig, "SIGSTOP"))
        return con.error(einvalid);
      
      // install a new signal
      sa.load(mem, act_p);
      opt.siglog("loaded new sigaction %s", sa.toString(mem));
    }
    
    return 0;
  }
  
  /**
   * Block signal. Delegates to sigprocmask.
   */
  public int rt_sigprocmask(int how, int ss_p, int oldss_p) {
    return sigprocmask(how, ss_p, oldss_p);
  }
  
  /**
   * Block signal
   * TODO need to print what signals are blocked
   */
  public int sigprocmask(int how, int ss_p, int oldss_p) {
    opt.info("sigprocmask(%s,%s,%s)", con.namerx("SIG_(BLOCK|UNBLOCK|SETMASK)", how), mem.getname(ss_p), mem.getname(oldss_p));
    int ss = ss_p != 0 ? mem.load_word(ss_p) : 0;
    int oldmask = signals.getmask();
    if (oldss_p != 0)
      mem.store_word(oldss_p, signals.getmask());
    
    if (con.is(how, "SIG_BLOCK")) {
      signals.setmask(oldmask | ss);
      opt.siglog("sigprocmask: %08x + %08x = %08x", oldmask, ss, signals.getmask());
    } else if (con.is(how, "SIG_UNBLOCK")) {
      signals.setmask(oldmask & ~ss);
      opt.siglog("sigprocmask: %08x - %08x = %08x", oldmask, ss, signals.getmask());
    } else if (con.is(how, "SIG_SETMASK")) {
      signals.setmask(ss);
      opt.siglog("sigprocmask: %08x = %08x", oldmask, ss);
    }
    // need to service if we just unblocked, it is done in Mips.call_fn
    return 0;
  }
  
  /**
   * Send signal to process
   */
  public int kill (int process, int sig) {
    opt.warn("kill(%d,%s)", process, con.name("SIG", sig));
    /*
    if (process == pid) {
      opt.warn("kill: self with %s", con.name("SIG", sig));
      if (sig == con.get("SIGKILL"))
        throw new EndOfProgramException();
    }
    */
    // TODO return error
    Machines.signal(process, sig);
    return 0;
  }
  
}
