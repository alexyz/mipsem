/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp.dev;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

import cfern.*;
import cfern.cpu.Machine;
import cfern.fs.*;
import cfern.io.*;
import cfern.sys.*;
import cfern.fs.tmp.*;

/**
 * Access to various special devices.
 * Device list is at http://www.lanana.org/docs/device-list/devices-2.6+.txt
 * TODO needs oss ioctls for controlling dsp sample rate etc
 * TODO /dev/full
 */
public class DeviceFileSystem extends TempFileSystem implements TerminalListener {
  
  private final static String name = "dev";
  
  /**
   * Factory for this file system
   */
  public static final Factory devfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) throws IOException {
      return new DeviceFileSystem(mount);
    }
    public String name() {
      return name;
    }
  };
  
  /**
   * Creates a device file system and fills with common devices.
   */
  protected DeviceFileSystem(String mount) throws IOException {
    // in future set tmpfs read only
    super(mount, null);
    Terminals.addTerminalListener(this);
    
    ChannelOpt tty = new ChannelOpt(5, 0) {
      protected Channel open() {
        Terminal tty = ((Machine) Thread.currentThread()).getTerminal();
        return new Channel(5, 0, tty);
      }
    };
    root.put("tty", tty);
    
    final StringReg dspOpt = new StringReg("22050hz 16b stereo -20db\n");
    root.put("dspopt", dspOpt);
    
    Stream dsp = new Stream(14, 3) {
      protected OutputStream out() {
        OutputStream os = OutputStreamUtil.sink;
        try {
          os = AudioOutputStream.newInstance(dspOpt.getString());
        } catch (IOException e) {
          e.printStackTrace();
        }
        return os;
      }
    };
    root.put("dsp", dsp);
    
    final Dir pts = new Dir();
    root.put("pts", pts);
    
    ChannelOpt ptmx = new ChannelOpt(5, 2) {
      int next = 0;
      protected Channel open() {
        int n = next++;
        try {
          Pipe p1 = Pipe.open(), p2 = Pipe.open();
          // need to cross in/out of pipe
          Channel m = new Channel(5, 2, new Terminal(p2.source(), p1.sink(), "master" + n, n));
          m.setfs(pts.getfs());
          Channel s = new Channel(136, 0, new Terminal(p1.source(), p2.sink(), "slave" + n, n));
          pts.put(Integer.toString(n), s);
          return m;
        } catch (IOException e) {
          throw new RuntimeException("could not create pts" + n, e);
        }
      }
    };
    root.put("ptmx", ptmx);
    
    //root.put("console", new FixedStream(5, 1, new Terminal("console", -1, InputStreamUtil.cin, OutputStreamUtil.cout)));
    //root.put("consoleraw", new FixedStream(InputStreamUtil.cinraw, OutputStreamUtil.cout, 5, 1));
    
    final Pipe consolePipe = Pipe.open();
    Thread con = new Thread("console-reader") {
      public void run() {
        byte[] buf = new byte[1024];
        try {
          while (true) {
            int r = InputStreamUtil.cin.read(buf);
            consolePipe.sink().write(ByteBuffer.wrap(buf, 0, r));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    con.setPriority(Thread.MAX_PRIORITY);
    con.setDaemon(true);
    con.start();
    
    // FIXME need another thread and writer for console output...
    root.put("console", new Channel(5, 1, new Terminal(consolePipe.source(), consolePipe.sink(), "console", -1)));
    
    root.put("exn", new FixedStream(5, 1, InputStreamUtil.exn, OutputStreamUtil.exn));
    root.put("ln1", new Link("/dev/tty"));
    root.put("ln2", new Link("/dev"));
    root.put("null", new FixedStream(1, 3, InputStreamUtil.eof, OutputStreamUtil.sink));
    root.put("random", new FixedStream(1, 8, InputStreamUtil.rand, OutputStreamUtil.sink));
    root.put("zero", new FixedStream(1, 5, InputStreamUtil.zeros, OutputStreamUtil.sink));
    
    Pipe pipe1 = Pipe.open();
    Pipe pipe2 = Pipe.open();
    root.put("pipe1", new Channel(0, 0, pipe2.source(), pipe1.sink()));
    root.put("pipe2", new Channel(0, 0, pipe1.source(), pipe2.sink()));

  }
  
  public void terminalCreated(Terminal tty) {
    Driver.opt().info("Dev: adding terminal %s", tty);
    root.put(tty.getname(), new Channel(4, 2, tty));
  }
  
  public String getShortName() {
    return name;
  }
  
}
