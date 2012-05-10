package cfern.sys;

/**
 * Listen for new terminals
 */
public interface TerminalListener {
  
  /**
   * Called for all listeners (i.e. /dev) when a new terminal is added
   */
  public void terminalCreated(Terminal t);
}
