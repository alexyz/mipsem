package cfern.cpu;

/**
 * Interface for receiving events about new, exited and renamed machines.
 */
public interface MachineListener {
  
  /**
   * Notify the terminal this process has started
   */
  public void machineStarted (int pid, String name);
  
  /**
   * Notify the terminal this process has exited
   */
  public void exited (int pid, int exit);
  
  /**
   * Update the process name after exec
   */
  public void machineExec (int pid, String name);
}
