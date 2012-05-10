/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.gui;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import javax.swing.*;
import cfern.Driver;

/**
 * A JTextArea with streams for printing and reading characters.
 * TODO end of file, ctrl-d
 * TODO need to catch control chars and send interrupts to process group
 * TODO right click copy/paste/clear menu, preferably reuseable, 
 * i.e. a guiterminal interface { copy, paste, clear }, though it should just call copy/paste action
 * 
 * In future: color terminal, subclass of JEditorPane?
 * Making a better terminal would probably involve subclassing AbstractDocument.Content 
 * and PlainDocument to support terminal escape sequences.
 */
public class SwingTerminal extends JTextArea {
  
  private final ChannelDocument doc;
  private final ChannelAppender reader;
  
  /**
   * Create a new swing terminal
   */
  public SwingTerminal() {
    super(new ChannelDocument(), "", 24, 80);
    this.doc = (ChannelDocument) getDocument();
    
    setBackground(Color.BLACK);
    setForeground(Color.WHITE);
    setCaretColor(Color.WHITE);
    setLineWrap(true);
    setFont(new Font("Monospaced", Font.PLAIN, 16));
    
    addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        // move to the end of document on return
        if (e.getKeyChar() == 10)
          setCaretPosition(doc.getLength());
      }  
    });
    
    reader = new ChannelAppender(new SwingTerminalAppendable());
    reader.start();
  }
  
  /**
   * Read typed/pasted chars from here
   */
  public ReadableByteChannel source() {
    return doc.source();
  }
  
  /**
   * Read typed/pasted chars from here
   */
  public WritableByteChannel sink() {
    return reader.sink();
  }
  
  /**
   * Just for testing the streams work.
   */
  public static void main (String[] args) throws Exception {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    SwingTerminal t = new SwingTerminal();
    f.add(t);
    f.pack();
    f.setVisible(true);
    ByteBuffer buf = ByteBuffer.wrap(new byte[1024]);
    while (true) {
      t.source().read(buf);
      buf.flip();
      t.sink().write(buf);
      buf.clear();
    }
  }
  
  /**
   * A class that reads bytes sent to sink() and writes them to the given
   * Appendable
   */
  class ChannelAppender extends Thread {
    private final Pipe pipe;
    private final Appendable app;
    
    ChannelAppender(Appendable app) {
      this.app = app;
      try {
        pipe = Pipe.open();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      setPriority(Thread.MAX_PRIORITY);
      setDaemon(true);
    }
    public void run() {
      CharsetDecoder dec = Driver.charset.newDecoder();
      ByteBuffer buf = ByteBuffer.wrap(new byte[1024]);
      char[] chars = new char[1024];
      CharBuffer cbuf = CharBuffer.wrap(chars);
      try {
        while (true) {
          pipe.source().read(buf);
          buf.flip();
          // less efficient
          // app.append(dec.decode(buf).toString());
          CoderResult r;
          do {
            r = dec.decode(buf, cbuf, false);
            cbuf.flip();
            app.append(new String(chars, 0, cbuf.limit()));
            cbuf.clear();
          } while (r == CoderResult.OVERFLOW);
          buf.clear();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    public WritableByteChannel sink() {
      return pipe.sink();
    }
  }
  
  class SwingTerminalAppendable implements Appendable {
    public Appendable append(char c) {
      throw new RuntimeException("unimplemented");
    }
    public Appendable append(CharSequence csq, int start, int end) {
      throw new RuntimeException("unimplemented");
    }
    public Appendable append(final CharSequence s) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          doc.insertStringSuper(s.toString());
          JTextArea textArea = SwingTerminal.this;
          textArea.setCaretPosition(doc.getLength());
        }
      });
      return this;
    }
  }
  
} // end of class SwingTerminal

