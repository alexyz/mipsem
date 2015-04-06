/**
 * Cfern, a MIPS/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 * 
 * TODO add aux data vector for ld.so.1
 * aux constants in http://rei/build/linux-2.6.9/include/linux/elf.h
 * and binfmt_elf line NEW_AUX_ENT(AT_HWCAP, ELF_HWCAP);
 *
 * TODO maybe exit queue should have a map of linked blocking queues?
 * 
 * BUG: cat from stdin says fdvalid: -1 (probably mmap)
 * BUG: directory symlinks don't work as path of a path
 * BUG: busybox161 kills emulator
 * BUG: busybox dd if=/dev/random of=x bs=1M count=1 (produces 16k file)
 * 
 * TODO terminal clear method
 * TODO terminal ansi filter
 * TODO /proc/window
 * TODO console pipe channel thread at high priority
 *      to flush: p.sink().write(enc.encode(CharBuffer.wrap("input")))
 *      plus keyboard commands, e.g. C-a c (int), C-a \ (quit), C-a w (window), C-a 1 (thread state)
 * TODO ChannelFileDesc class, no is/os wrappers
 *      to read: p.source().read(ByteBuffer.wrap(buf, off, len))
 * TODO Channel - new tmp fs type
 */

package cfern;
import java.awt.EventQueue;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import cfern.fs.*;
import cfern.cpu.*;
import cfern.cpu.mips.Mips;
import cfern.gui.*;
import cfern.sys.Constants;
import cfern.elf.*;

/**
 * Driver functions for starting the emulation.
 * Also keeps track of each machine so they can send each other signals.
 * Note: this is the only source code file to use UTF-8.
 */
public class Driver {
  
  /**
   * Charset to use for target from/to host string conversion.
   * TODO more efficient conversion functions, e.g. returning a byte/char iterator
   */
  // could read env for this
  public static final Charset charset = Charset.forName("UTF-8");
  
  /**
   * Global options
   */
  public static final Options opt = new Options();
  
  /**
   * Enviroment
   */
  // should perhaps be a map
  public static final ArrayList<String> env = new ArrayList<String>();
  
  private static String shell = null;
  
  /**
   * main, checks arguments, creates initial elfloader, starts the machine running
   * (uses a thread)
   */
  public static void main (String[] args) throws Exception {
    System.setProperty("swing.aatext", "true");
    System.err.println("cwd is " + System.getProperty("user.dir"));
    
    if (new File("sbin/busybox-mips").exists())
      shell = "/usr/sbin/busybox-mips";
    else if (new File("cfern.jar").exists())
      shell = "/jar/sbin/busybox-mips";
    else
      throw new RuntimeException("can't find sbin/busybox-mips or cfern.jar");
    
    putenv("PATH=/bin:/usr/sbin:/usr/sbin2:/jar/sbin");
    putenv("HOME=/");
    putenv("CWD=/");
    // need this otherwise ash gets confused when doing "cd dev"
    putenv("PWD=/");
    putenv("SHELL=" + shell);
    putenv("USER=root");
    putenv("UTF8_TEST=日本語");
    
    boolean gui = false;
    // argument index
    int i = 0;
    
    while (i < args.length) {
      String a = args[i];
      
      if (a.charAt(0) == '-') {
        if (a.indexOf('h') >= 0) {
          usage();
          return;
        }
        opt.disasm = a.indexOf('d') >= 0;
        opt.interactive = a.indexOf('i') >= 0;
        opt.regprint = a.indexOf('r') >= 0;
        opt.funlog = a.indexOf('f') >= 0;
        opt.slow = a.indexOf('s') >= 0;
        
        opt.warn = a.indexOf('1') >= 0;
        opt.info = a.indexOf('2') >= 0;
        opt.debug = a.indexOf('3') >= 0;
        opt.trace = a.indexOf('4') >= 0;
        opt.undef = a.indexOf('!') >= 0;
        
        gui = a.indexOf('w') >= 0;
        i++;
        
      } else if (a.indexOf('=') > 0) {
        putenv(a);
        i++;
        
      } else {
        // get program name
        break;
      }
    }
    
    opt.init(env);
    
    Constants.load("glibc.txt");
    
    // mount file systems
    FileSystem vfs = FileSystem.get();
    vfs.mount("tmp", null, "/");
    vfs.mkdir("/dev", 0, 0);
    vfs.mount("dev", null, "/dev");
    vfs.mkdir("/usr", 0, 0);
    vfs.mount("nat", System.getProperty("user.dir"), "/usr");
    vfs.mkdir("/proc", 0, 0);
    vfs.mount("proc", null, "/proc");
    if (new File("C:\\").exists()) {
      vfs.mkdir("/c", 0, 0);
      vfs.mount("nat", "C:\\", "/c");
    }
    if (new File("cfern.jar").exists()) {
      vfs.mkdir("/jar", 0, 0);
      vfs.mount("zip", "cfern.jar", "/jar");
    }
    vfs.mkdir("/etc", 0, 0);
    vfs.mount("etc", null, "/etc");
    vfs.mkdir("/bin", 0, 0);
    vfs.symlink(shell, "/bin/ls");
    
    String tty = "/dev/console";
    if (gui) {
      // note: need to mount dev before creating consoles
      tty = "/dev/" + SwingConsole.getInstance().newAction();
    }
    
    // get target program arguments
    ArrayList<String> userargs = new ArrayList<String>();
    if (i == args.length) {
      // no program or args, start default shell
      userargs.add(shell);
      userargs.add("ash");
      // make it run /etc/profile
      userargs.add("--login");
    } else {
      while (i < args.length) {
        String arg = args[i++];
        // TODO make this work for linux paths too
        if (FileSystemUtil.isWindowsPath(arg)) {
          // convert native path to unix path
          String uarg = vfs.unixPathFor(arg);
          if (uarg != null)
            arg = uarg;
        }
        userargs.add(arg);
      }
    }
    
    start(tty, userargs, env);
  }
  
  /**
   * Start a mips thread on the given terminal with given args and env.
   * If terminal is null, one is not opened.
   * If args are null or empty, busybox ash is started.
   * If env is null, default enviroment is used.
   */
  public static void start(final String tty, List<String> args, List<String> env) throws Exception {
    // no program or args, start default shell
    if (args == null)
      args = new ArrayList<String>(2);
    if (args.size() == 0) {
      // should do something more sensible than this
    	if (shell == null) {
    		throw new Exception("no shell");
    	}
      args.add(shell);
      args.add("ash");
    }
    if (env == null)
      env = Driver.env;
    String prog = args.get(0);
    
    // loads elf file, creates symbol table
    ElfLoader elf;
    try {
      String aprog = prog.startsWith("/") ? prog : "/".concat(prog);
      FileDesc uf = FileSystem.get().openex(aprog);
      elf = ElfLoader.loadelf(uf);
    } catch (IOException e) {
    	e.printStackTrace();
    	throw new Exception("Driver: Could not load " + prog);
    }
    
    // create machine with pid 1 and ppid 0
    // in future should check machine type
    Machine mips = new Mips();
    if (tty != null)
      mips.opentty(tty);
    
    mips.load(elf, args, env);
    mips.start();
  }
  
  /**
   * Put or replace an enviroment variable
   */
  public static void putenv(String s) {
    String name = s.substring(0, s.indexOf('='));
    for (int n = 0; n < env.size(); n++) {
      if (env.get(n).startsWith(name)) {
        env.set(n, s);
        return;
      }
    }
    env.add(s);
  }
  
  /**
   * Print a usage statement
   */
  private static void usage() {
    String usage = "Cfern - a MIPS/Unix/C emulator for Java\n" +
        "Alexander Slack alexslack56@hotmail.com\n" +
        "Usage:\n" +
        "  java cfern.Driver [-opts] [env=val] progname [userargs]\n" +
        "Opts:\n" +
        "  -d  disassemble\n" +
        "  -i  single step\n" +
        "  -r  print regs\n" +
        "  -s  debug mode\n" +
        "  -w  create console window\n" + 
        "  -1  print warning messages\n" +
        "  -2  print info messages\n" +
        "  -3  print debug messages\n" +
        "  -4  print trace messages\n" +
        "  -c  don't strip carriage returns from console input\n" +
        "  -!  ignore undefined constants\n" +
        "Env:\n" +
        "  bp=function,...     do a disasm breakpoint in f and ...\n" +
        "  log=fs,sig,fun,elf  enable info logging of subsystem\n";
    
    System.err.print(usage);
  }
  
  /**
   * Make a single step in interactive mode
   * TODO move this to Terminal
   */
  public static void step() {
    try {
      while (System.in.read() != 10) {
        //
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Java has no primitive type subarray to string method
   */
  public static String toString(byte[] buf, int start, int end) {
    StringBuilder sb = new StringBuilder("[");
    for (int n = start; n < end; n++)
      sb.append(buf[n]).append(", ");
    if (sb.length() >= 2)
      sb.setLength(sb.length() - 2);
    sb.append("]");
    return sb.toString();
  }
  
  public static int indexOf(Object[] a, Object o) {
    for (int n = 0; n < a.length; n++)
      if (a[n] == o)
        return n;
    return -1;
  }
  
  public static int indexOfSafe(Object[] a, Object o) {
    return a == null ? -1 : indexOf(a, o);
  }

  /**
   * Return the options/logger for the current thread.
   * Returns global options if thread options not set yet.
   */
  public static Options opt() {
    Thread t = Thread.currentThread();
    return t instanceof Machine ? ((Machine) t).getopt() : opt;
  }
  
  /**
   * Append a short to a short array, returns new array
   */
  public static short[] append(short[] a, short s) {
    if (a == null) {
      a = new short[] { s };
    } else {
      a = Arrays.copyOf(a, a.length + 1);
      a[a.length - 1] = s;
    }
    return a;
  }
  
} // end of class Driver
