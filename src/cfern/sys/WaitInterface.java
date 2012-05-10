/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;

/**
 * Methods for <sys/stat.h>
 */
public class WaitInterface extends Interface {
  
  /**
   * Queue of exited children. Never null.
   * Children inherit this as exqparent.
   */
  final ExitQueue exqchild = new ExitQueue();
  
  public WaitInterface(SystemInterface sys) {
    super(sys);
  }
  
  /**
   * Delegates to wait4
   */
  public int waitpid(int wpid, int status_p, int options) {
    return wait4(wpid, status_p, options, 0);
  }
  
  /**
   * Delegates to wait4
   */
  public int waitchild(int status_p) {
    return wait4(-1, status_p, 0, 0);
  }
  
  /**
   * wait for process wpid to exit
   */
  public int wait4(int wpid, int status_p, int options, int rusage_p) {
    opt.info("wait4 (pid %d, status %s, opt %s, rusage %s)", wpid, mem.getname(status_p), con.names("W", options), mem.getname(rusage_p));
    ExitValue ex;
    boolean nohang = con.has(options, "WNOHANG");
    boolean untraced = con.has(options, "WUNTRACED");
    
    if (wpid == -1) {
      // wait for any child, may block
      ex = exqchild.take();
      if (ex == null) {
        // seems to happen frequently in busybox lash
        opt.info("wait4: no children");
        // if WNOHANG then should return 0 instead of ECHILD
        // TODO not sure about untraced
        return nohang || untraced ? 0 : con.error(echild);
      }
    } else {
      // wait for specific child
      // FIXME should remove from exq rather than exqchild?
      // and maybe have a consume() method to avoid getting the same exit from both
      ex = exqchild.take(wpid, true);
      if (ex == null) {
        opt.warn("wait4: could not find child %d", wpid);
        return nohang ? 0 : con.error(echild);
      }
    }
    
    opt.debug("wait4 done, exit of pid %d is %d", ex.pid(), ex.exit());
    if (rusage_p != 0)
      opt.warn("wait4: stub not filling in rusage"); 
    // ex may be null
    // FIXME status needs to support macros
    //int exited = ((0 & 0x7f) == 0);
    //int status = ((0 & 0xff00) >> 8);
    //int sig = (((signed char) ((0 & 0x7f) + 1) >> 1) > 0);
    //int core = (0 & 0x80);
    //int stop = ((0 & 0xff) == 0x7f);

    int status = (ex.exit() & 0xff) << 8;
    mem.store_word(status_p, status);
    return ex.pid();
  }
  
}
