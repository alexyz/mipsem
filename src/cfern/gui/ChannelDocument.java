/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.gui;
import java.nio.channels.ReadableByteChannel;
import javax.swing.text.*;
import cfern.io.*;

/**
 * Catches changes to the document, vetos invalid ones and sends the rest to the
 * key buffer which can be read from "in"
 * Should maybe be called DocumentInputStream
 * TODO create AbstractDocument/Content subclass with methods for move/overwrite, other ansi stuff - yeah right
 */
public class ChannelDocument extends PlainDocument {
  
  /**
   * Read the key buffer from here
   */
  private final StringChannel in;
  
  /**
   * Create a new InputStreamDocument for use within a JTextArea
   */
  public ChannelDocument() {
    in = new StringChannel();
  }
  
  public ReadableByteChannel source() {
    return in.source();
  }
  
  /**
   * Inserts a string without sending it to keyBuf.
   * Need this for emulator output being written to the console.
   */
  public void insertStringSuper(String str) {
    try {
      super.insertString(getLength(), str, null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Insert a string into the document and keybuffer.
   */
  public void insertString(int off, String str, AttributeSet a) throws BadLocationException {
    if (str.length() == 0)
      return;
    
    // TODO need to check if local echo is enabled...
    if (in.insert(getLength() - off, str))
      super.insertString(off, str, a);
  }
  
  /**
   * Delete characters from the document and the key buffer.
   */
  public void remove(int off, int len) throws BadLocationException {
    if (in.remove(getLength() - off, len))
      super.remove(off, len);
  }
  
} // end of class ConsoleDocument
