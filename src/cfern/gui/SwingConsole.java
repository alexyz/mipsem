/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.gui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import cfern.Driver;
import cfern.sys.*;

/**
 * A frame with a process list and a tabbed list of terminals
 * TODO need a help tab, list build.txt?
 * TODO a log message tab with filters
 */
public class SwingConsole extends JFrame {
  
  private final JTabbedPane tabs;
  private final SwingProcessList list;
  
  /**
   * Create and display a new swing console
   */
  public SwingConsole() {
    super("Cfern Console");
    // default layout is BorderLayout
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    tabs = new JTabbedPane();
    tabs.setFocusable(false);
    add(tabs, BorderLayout.CENTER);
    newAction();
    
    list = new SwingProcessList();
    JScrollPane listScroller = new JScrollPane(list);
    listScroller.setPreferredSize(new Dimension(200, 200));
    add(listScroller, BorderLayout.WEST);
    
    setJMenuBar(initMenu());
    
    pack();
    setVisible(true);
  }
  
  private JMenuBar initMenu() {
    JMenuBar mb = new JMenuBar();
    JMenu m = new JMenu("Terminal");
    mb.add(m);
    JMenuItem newItem = new JMenuItem("New Terminal");
    newItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newAction();
      }
    });
    m.add(newItem);
    JMenuItem newAshItem = new JMenuItem("New Terminal + Shell");
    newAshItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newAshAction();
      }
    });
    m.add(newAshItem);
    JMenuItem exitItem = new JMenuItem("Exit");
    exitItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        exitAction();
      }
    });
    m.add(exitItem);
    return mb;
  }
  
  /**
   * Just exit
   */
  private void exitAction() {
    // TODO should confirm if there are machines running
    System.exit(0);
  }
  
  /**
   * Create a new terminal
   */
  private String newAction() {
    int num = tabs.getTabCount() + 1;
    String name = "tty" + num;
    SwingTerminal sterm = new SwingTerminal();
    
    JScrollPane stermScroll = new JScrollPane(sterm, 
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    tabs.addTab("/dev/" + name, stermScroll);
    
    Terminal term = new Terminal(sterm.source(), sterm.sink(), name, -1);
    Terminals.addTerminal(term);
    return name;
  }
  
  /**
   * Create a new terminal + start ash
   */
  private void newAshAction() {
    String tty = newAction();
    Driver.start("/dev/" + tty, null, null);
    // doesn't focus terminal
    //tabs.setSelectedIndex(tabs.getTabCount() - 1);
  }
    
} // end of class SwingConsole
