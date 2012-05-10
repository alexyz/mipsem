/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.util.*;

import cfern.Driver;

/**
 * Allows processes to wait for other processes to exit
 */
class ExitQueue {
  
  /**
   * Started processes (a set, stored in Vector for efficiency)
   */
  private final List<Integer> started = new Vector<Integer>();
  /**
   * Exited processes (a queue)
   */
  private final LinkedList<ExitValue> exited = new LinkedList<ExitValue>();
  
  /**
   * Must be called after a new process is created but BEFORE it is started.
   * All new processes should be added to both global exit queue and local exit queue.
   */
  synchronized void start(int pid) {
    Integer pidObj = Integer.valueOf(pid);
    started.add(pidObj);
  }
  
  /**
   * Called by SystemInterface.exit when a process exits.
   * Any other processes waiting will be notified.
   */
  synchronized void exit(ExitValue ex) {
    started.remove(Integer.valueOf(ex.pid()));
    exited.add(ex);
    notifyAll();
  }
  
  /**
   * Internal helper for waiting until a notify while handling signals
   */
  private void waitLoop() {
    boolean wait = true;
    while (wait) {
      try {
        wait();
        wait = false;
      } catch (InterruptedException e) {
        Driver.opt().error("interrupted in take");
        // won't run until after we finish waiting..
        //((cfern.cpu.Machine) Thread.currentThread()).service();
      }
    }
  }
      
  /**
   * For local exit queues only (i.e. only one thread).
   * Return the last or wait for the next exited child.
   * Returns null if there are no children.
   */
  synchronized ExitValue take() {
    if (exited.size() > 0)
      return exited.poll();
    if (started.size() == 0) {
      // busybox lash does this alot
      //Driver.opt().info("ExitQueue.take: No children");
      return null;
    }
    waitLoop();
    return exited.poll();
  }
  
  /**
   * Wait for the exit of given pid. Local exit queues should remove. Returns
   * null if there is no such pid.
   */
  synchronized ExitValue take(int pid, boolean remove) {
    Integer pidObj = Integer.valueOf(pid);

    // see if specified pid has already exited
    Iterator<ExitValue> iter = exited.iterator();
    while (iter.hasNext()) {
      ExitValue oldex = iter.next();
      if (oldex.pid() == pid) {
        if (remove)
          iter.remove();
        return oldex;
      }
    }

    // see if it's started
    if (!started.contains(pidObj)) {
      Driver.opt().error("pid %d not started or exited", pid);
      return null;
    }

    // wait for each exit and check
    while (started.contains(pidObj)) {
      waitLoop();
      // exited processes are added to the end, pulled from the start
      ExitValue ex = exited.getLast();
      if (ex.pid() == pid) {
        if (remove)
          exited.poll();
        return ex;
      }
    }

    Driver.opt().error("did not catch exit of %d", pid);
    return null;
  }
  
}
