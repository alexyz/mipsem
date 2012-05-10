/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.*;

import cfern.Driver;

/**
 * Prevents closing of an output stream
 */
class ConsoleOutputStream extends FilterOutputStream {
  
  ConsoleOutputStream(OutputStream out) {
    super(out);
  }
  
  public void close() {
    Driver.opt().fslog("ignored attempt to close console output");
  }
  
  public void write(byte[] buf, int off, int len) throws IOException {
    out.write(buf, off, len);
  }
}
