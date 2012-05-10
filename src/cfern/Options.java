/**
 * Cfern, a MIPS/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern;

import java.util.List;

/**
 * Runtime options and message logging.
 * This class is thread safe.
 */  
public class Options {
  
  /**
   * Disassembly options
   */
  public boolean interactive, disasm, regprint;
  
  /**
   * Syscall log level
   */
  public boolean slow, trace, debug, info, warn;
  
  /**
   * ELF loading logging, file system logging, signal logging and function call
   * logging
   */
  public boolean elflog, fslog, siglog, funlog;
  
  /**
   * Misc
   */
  public boolean keepcr, undef;
  
  public Options() {
    // yawn
  }
  
  /**
   * Loads options from enviroment, other initialisation
   */
  public void init(List<String> env) {
    for (int n = 0; n < env.size(); n++) {
      String s = env.get(n);
      if (s.startsWith("log=")) {
        elflog = s.indexOf("elf") > 0;
        fslog = s.indexOf("fs") > 0;
        siglog = s.indexOf("sig") > 0;
        funlog = s.indexOf("fun") > 0;
      }
    }
    if (trace) {
      debug = true;
    }
    if (debug) {
      elflog = true;
      siglog = true;
      fslog = true;
      info = true;
    }
    if (info) {
      warn = true;
    }
    // also if any breakpoints are set
    slow = slow || disasm || interactive || regprint || funlog;
  }
  
  /**
   * Copy an Options object
   */
  private Options(Options other) {
    debug = other.debug;
    disasm = other.disasm;
    elflog = other.elflog;
    fslog = other.fslog;
    funlog = other.funlog;
    info = other.info;
    interactive = other.interactive;
    regprint = other.regprint;
    slow = other.slow;
    siglog = other.siglog;
    trace = other.trace;
    undef = other.undef;
    warn = other.warn;
  }
  
  /**
   * The elf loading log
   */
  public void elflog(String format, Object... args) {
    println(elflog, "[elf] ", format, args);
  }
  
  /**
   * The signal log
   */
  public void siglog(String format, Object... args) {
    println(siglog, "[sig] ", format, args);
  }
  
  /**
   * The file system log
   */
  public void fslog(String format, Object... args) {
    println(fslog, "[fs] ", format, args);
  }
  
  /**
   * Print excessive detail on all syscalls and function calls
   */
  public void trace(String format, Object... args) {
    println(trace, "[trace] ", format, args);
  }
  
  /**
   * All system calls
   */
  public void debug(String format, Object... args) {
      println(debug, "[debug] ", format, args);
  }
  
  /**
   * Most system calls
   */
  public void info(String format, Object... args) {
      println(info, "[info] ", format, args);
  }
  
  /**
   * Unexpected stuff only
   */
  public void warn(String format, Object... args) {
      println(warn, "[WARN] ", format, args);
  }
  
  /**
   * Internal errors
   */
  public void error(String format, Object... args) {
    println(true, "[ERROR] ", format, args);
  }
  
  /**
   * Print an unconditional message that includes thread name
   */
  public void println(String format, Object... args) {
    println(true, "", format, args);
  }
  
  /**
   * print a debug message that includes thread name
   */
  public void println(boolean print, String prefix, String format, Object... args) {
    // newline is automatic...
    if (format.endsWith("\n")) {
      System.err.println("don't use \\n in log messages...");
      Thread.dumpStack();
    }
    if (print) {
      String name = Thread.currentThread().getName();
      System.err.printf("{%s} %s%s\n", name, prefix, String.format(format, args));
    }
  }
  
  /**
   * Copy an Options object so that it can be changed by another thread
   */
  public Options copy() {
    return new Options(this);
  }
  
}