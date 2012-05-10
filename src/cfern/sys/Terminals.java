package cfern.sys;

import java.util.*;

/**
 * Terminal listeners
 */
public class Terminals {
  
  private final static ArrayList<TerminalListener> listeners = new ArrayList<TerminalListener>();
  
  /**
   * Register for notifications of new GUI terminals.
   */
  public synchronized static void addTerminalListener(TerminalListener tl) {
    if (!listeners.contains(tl))
      listeners.add(tl);
  }
  
  /**
   * Register a new GUI terminal
   */
  public synchronized static void addTerminal(Terminal t) {
    if (listeners.size() == 0)
      throw new RuntimeException("new terminal; nobody cared");
    for (TerminalListener tl : listeners)
      tl.terminalCreated(t);
  }
  
}
