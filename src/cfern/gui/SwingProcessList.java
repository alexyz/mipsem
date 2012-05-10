/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.gui;
import java.awt.event.*;
import javax.swing.*;
import cfern.cpu.*;
import cfern.sys.Constants;

/**
 * A self updating process list.
 * TODO right click on list doesnt change selection
 */
public class SwingProcessList extends JList implements ActionListener, MachineListener {
  
  private final DefaultListModel model;
  private final JPopupMenu menu;
  private final Timer timer;
  
  /**
   * Create a new swing console
   */
  public SwingProcessList() {
    model = new DefaultListModel();
    setModel(model);
    setFocusable(false);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    menu = new JPopupMenu();
    addMenuItem("Interrupt", "SIGINT");
    addMenuItem("Quit", "SIGQUIT");
    addMenuItem("Kill", "SIGKILL");
    addMenuItem("Stop", "SIGSTOP");
    addMenuItem("Continue", "SIGCONT");
    setComponentPopupMenu(menu);

    /*
    MouseListener mouseListener = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          int index = locationToIndex(e.getPoint());
          setSelectedIndex(index);
          // TODO show popup
        }
      }
    };
    addMouseListener(mouseListener);
    */

    // TODO might be better to have this as a low priority
    // non awt thread that polls more frequently
    timer = new Timer(5000, this);
    timer.start();
    
    Machines.addMachineListener(this);
  }
  
  /**
   * Add an item to the pop up menu
   */
  private void addMenuItem(String name, String command) {
    JMenuItem item = new JMenuItem(name);
    item.setActionCommand(command);
    item.addActionListener(this);
    menu.add(item);
  }
  
  /**
   * visually update the state of all processes in the list
   */
  private void update() {
    boolean changed = false;
    for (int n = 0; n < model.size(); n++) {
      ListItem item = (ListItem) model.get(n);
      if (item.update())
        changed = true;
    }
    if (changed)
      repaint();
  }
  
  /**
   * Add a new machine to the list
   */
  public void machineStarted(final int pid, final String name) {
    final String state = Machines.state(pid);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ListItem item = new ListItem(pid, name, state);
        model.addElement(item);
        if (getSelectedIndex() == -1)
          setSelectedIndex(0);
      }
    });
    //validate();
  }
  
  /**
   * Update name of machine
   */
  public void machineExec(final int pid, final String name) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (int n = 0; n < model.size(); n++) {
            ListItem item = (ListItem) model.get(n);
            if (item.pid == pid)
              item.setname(name);
          }
          repaint();
        }
      });
    }
  
  /**
   * Remove or grey out a machine from the list
   */
  public void exited(final int pid, final int exit) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        for (int n = 0; n < model.size(); n++) {
          ListItem item = (ListItem) model.get(n);
          if (item.pid == pid)
            item.setexited(exit);
        }
        repaint();
      }
    });
  }
  
  /**
   * Respond to menu commands
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == timer) {
      update();
      return;
    }
    
    String com = e.getActionCommand();
    // need this but only for mouse events...
    //int index = locationToIndex(e.getPoint());
    ListItem item = (ListItem) getSelectedValue();
    if (item == null || com == null)
      return;
    
    if (com.startsWith("SIG"))
      Machines.signal(item.pid, Constants.get().get(com));
  }
  
} // end of class Console

/**
 * Item representing machine in JList
 */
class ListItem {
  /**
   * pid of machine (never changes)
   */
  public final int pid;
  /**
   * name of machine (may change)
   */
  private String name;
  /**
   * state of machine (may change)
   */
  private String state;
  
  ListItem(int pid, String name, String state) {
    this.pid = pid;
    this.name = name;
    this.state = state;
  }
  
  public void setexited(int exit) {
    this.state = "EXIT " + exit;
  }
  
  public void setname(String name) {
    this.name = name;
  }
  
  /**
   * update thread state, return true if state changed
   */
  public boolean update() {
    if (!state.startsWith("EXIT")) {
      String newstate = Machines.state(pid);
      if (!state.equals(newstate)) {
        state = newstate;
        return true;
      }
    }
    return false;
  }
  
  public String toString() {
    return "[" + pid + "]  " + name + " - " + state;
  }
}

