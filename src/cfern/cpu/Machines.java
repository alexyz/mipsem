/**
 * Cfern, a MIPS/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;
import java.util.*;


/**
 * Keeps track of running machines, also notifies terminal of changes in machine
 * state. All methods are synchronized to avoid concurrent modification.
 */
public final class Machines {
  
  /**
   * Running machines.
   */
  private static final Map<Integer,Machine> machines = new TreeMap<Integer,Machine>();
  
  /**
   * Listeners for new/changes machines (i.e. the gui)
   * TODO use weakrefs
   */
  private static final List<MachineListener> listeners = new Vector<MachineListener>();
  
  /**
   * Next free process
   */
  private static int nextPid = 0;
  
  /**
   * Hide from javadoc
   */
  private Machines() {
    // yawn
  }
  
  /**
   * Allocate a new pid.
   */
  public static synchronized int newpid() {
    int pid = nextPid++;
    // should reuse pids in this case
    if (pid >= 65536)
      throw new RuntimeException("no more process ids");
    return pid;
  }
  
  /**
   * Add something that wants to know about new and exited Machines.
   */
  public static synchronized void addMachineListener(MachineListener term) {
    if (!listeners.contains(term))
      listeners.add(term);
  }
  
  /**
   * Keep a reference to this machine so we an send signals to it.
   * Also update user interface.
   */
  public static synchronized void started(Machine machine) {
    int pid = machine.getpid();
    machines.put(Integer.valueOf(pid), machine);
    for (MachineListener l : listeners)
      l.machineStarted(pid, machine.getName());
  }
  
  /**
   * Update the name of this machine on execve
   */
  public static synchronized void update(Machine m) {
    int pid = m.getpid();
    if (!machines.containsKey(Integer.valueOf(pid)))
      throw new RuntimeException("could not find " + m);
    // could pass old name
    for (MachineListener l : listeners)
      l.machineExec(pid, m.getName());
  }
  
  /**
   * Indicate this machine is no longer running
   */
  public static synchronized void exited(int pid, int exit) {
    Machine m = machines.remove(Integer.valueOf(pid));
    if (m == null)
      throw new RuntimeException("pid " + pid + " wasn't running");
  }
  
  /**
   * Send a 'signal' to a given 'process id'.
   * Signal 0 is used to see if a process exists.
   * Returns true if process exists.
   */
  public static synchronized boolean signal(int pid, int sig) {
    // TODO: pid -1 means send to all.
    if (pid < 0)
      throw new RuntimeException("signal all processes unimplemented");
    
    Machine m = machines.get(pid);
    if (m != null && m.isAlive()) {
      if (sig > 0)
        m.signal(sig);
      return true;
    }
    return false;
  }
  
  /**
   * Get the thread state of the given process
   */
  public static synchronized String state(int pid) {
    Machine m = machines.get(pid);
    return (m != null) ? m.getState().toString() : "EXITED";
  }
  
} // end of class Machines
