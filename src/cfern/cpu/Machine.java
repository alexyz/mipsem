/*
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;

import java.util.List;
import cfern.*;
import cfern.elf.ElfLoader;
import cfern.mem.*;
import cfern.sys.*;
import cfern.sys.str.SigAction;
import static cfern.cpu.Syscalls.*;

/**
 * A Thread for machine independent processor emulation.
 * Methods in this class are NOT thread safe (except signal and getpid).
 */
public abstract class Machine extends Thread {
  
  /**
   * Unix system interface. Note this maintains pid
   */
  protected final SystemInterface sys;
  
  /**
   * Memory interface
   */
  protected final Memory mem;
  
  /**
   * Options and debug logger
   */
  protected final Options opt;
  
  /**
   * Function call logger, to produce backtraces
   */
  protected final FunctionLog fun;
  
  /**
   * Create a new Machine with the given options.
   * Only called from Driver.main
   */
  protected Machine() {
    opt = Driver.opt().copy();
    mem = Memory.make();
    sys = new SystemInterface(mem, opt);
    fun = new FunctionLog(mem);
  }
  
  /**
   * Make a new Machine by copying the given Machine, as part of fork.
   */
  protected Machine(Machine other) {
    opt = other.opt.copy();
    mem = other.mem.copy();
    sys = new SystemInterface(other.sys, mem, opt);
    fun = new FunctionLog(mem);
  }
  
  /**
   * Open terminal as fd 0, 1 and 2.
   * This method must be called at most once before the thread starts.
   */
  public void opentty(String ttyname) {
    sys.fcntl().openTerminal(ttyname);
  }
  
  /**
   * Get the current terminal
   */
  public Terminal getTerminal() {
    return sys.fcntl().getTerminal();
  }
  
  /**
   * Get options
   */
  public Options getopt() {
    return opt;
  }
  
  /**
   * Get the process id from the system interface
   */
  public int getpid() {
    return sys.unistd().getpid();
  }
  
  /**
   * Signal this machine from a thread external to the machine (or the same
   * thread, but this should be rare).
   */
  public final synchronized boolean signal(int sig) {
    SignalHandler signals = sys.signal().getSignalHandler();
    String signame = Constants.get().name("SIG", sig);

    boolean intr = signals.queue(sig);
    if (Thread.currentThread() == this) {
      opt.info("signal: got %s from self, service: %s", signame, intr);
      if (intr)
        service();
    } else {
      opt.siglog("signal: sending %s to %s, will interrupt: %s", signame, getName(), intr);
      if (intr)
        interrupt();
    }

    return true;
  }
  
  /**
   * Service an interrupt. Called from this thread when it realises it has been
   * interrupted. NOTE: this does not "run" an interrupt handler, it only saves
   * state and sets up the signal state. Thus this may only be called once per
   * instruction cycle and only after any state changes have been made for the
   * current cycle.
   */
  protected synchronized void service() {
    if (Thread.currentThread() != this)
      throw new RuntimeException("attempt to service signal from outside machine thread");

    if (isInterrupted())
      interrupted();
    SignalHandler sig = sys.signal().getSignalHandler();
    SigAction act = sig.take();
    // this happens at end of syscall handler in most cases
    if (act == null)
      return;

    if (act.exit()) {
      opt.warn("service: exiting on %s", act.toString(mem));
      throw new EndOfProgramException();
    }

    // TODO: send signal and exit status to parent on child stop/continue
    // but only if specified in sigchld action...
    if (act.stop()) {
      int oldmask = sig.getmask();
      sig.setmask(act.getmask());
      opt.warn("service: stopping");
      try {
        // wait for cont or kill interrupt
        wait();
      } catch (InterruptedException e) {
        opt.warn("service: stop interrupted");
      }
      sig.setmask(oldmask);
      // needs to recurse here, but what if it's another stop?

    } else if (!act.cont()) {
      // actually invoke a user signal handler
      invoke(act);
    }
  }

  /**
   * Tell the implementation to run this signal handler now.
   */
  protected abstract void invoke(SigAction act);
  
  /**
   * Load the memory of this machine with given elf file and arguments. Must only
   * be called before starting or from this thread.
   */
  public abstract void load(ElfLoader elf, List<String> args, List<String> env);
  
  /**
   * Fork this machine. Must only be called from this thread.
   */
  public abstract Machine copy();
  
  /**
   * Restore registers after signal
   */
  protected abstract void restore();
  
  /**
   * Run the implementation dependent instruction scheduler
   */
  protected abstract byte runImpl();
  
  /**
   * Start the instruction scheduler
   */
  public final void run() {
    Machines.started(this);
    opt.warn("program start: %s scheduler, %s memory", opt.slow ? "debug" : "fast", mem.getClass().getSimpleName());
    
    long t = System.currentTimeMillis();
    byte exitval = runImpl();
    t = System.currentTimeMillis() - t;
    opt.warn("program exit: returns %d, took %d ms", exitval, t);
    
    // allow pages to be reused
    mem.clear();
    opt.warn(Pages.string());
    
    //System.err.println(mem);
    sys.exit(exitval);
    Machines.exited(getpid(), exitval);
  }
  
  /**
   * Do given system call with arguments.
   * Note: other architechtures may have different syscall numbers.
   */
  protected final int syscall(int call, int a, int b, int c, int d, int e, int f) {
    //System.err.println("syscall " + call + " " + sys_names[call].name);
    
    switch (call) {
      case SYS_SYSCALL:
        return syscall(a, b, c, d, e, f, 0);
      case SYS_EXIT:
        throw new EndOfProgramException();
      case SYS_FORK:
        // doesnt return from here in the forked program - goes straight to run()
        return sys.fork(copy());
      case SYS_READ:
        return sys.unistd().read(a, b, c);
      case SYS_WRITE:
        return sys.unistd().write(a, b, c);
      case SYS_OPEN:
        return sys.fcntl().open(a, b, c);
      case SYS_CLOSE:
        return sys.unistd().close(a);
      case SYS_WAITPID:
        return sys.wait_().waitpid(a, b, c);
      case SYS_LINK:
        return sys.unistd().link(a, b);
      case SYS_UNLINK:
        return sys.unistd().unlink(a);
      case SYS_EXECVE:
        return sys.unistd().execve(this, a, b, c);
      case SYS_CHDIR:
        return sys.unistd().chdir(a);
      case SYS_CHMOD:
        return sys.stat().chmod(a, b);
      case SYS_STAT:
        return sys.stat().stat(a, b);
      case SYS_GETPID:
        return sys.unistd().getpid();
      case SYS_PIPE:
        return sys.unistd().pipe(a);
      case SYS_FSTAT:
        return sys.stat().fstat(a, b);
      case SYS_TIME:
        return sys.time(a);
      case SYS_GETPPID:
        return sys.unistd().getppid();
      case SYS_GETDENTS:
        return sys.getdents(a,b,c);
      case SYS_FCNTL:
        return sys.fcntl().fcntl(a,b,c);
      case SYS_KILL:
        return sys.signal().kill(a,b);
      case SYS_LSEEK:
        return sys.unistd().lseek(a,b,c);
      case SYS_GETTIMEOFDAY:
        return sys.gettimeofday(a,b);
      case SYS_MKDIR:
        return sys.stat().mkdir(a,b);
        
        // glibc syscalls
        
      case SYS_UNAME:
        return sys.uname(a);
      case SYS_GETEUID:
        return sys.unistd().geteuid();
      case SYS_GETUID:
        return sys.unistd().getuid();
      case SYS_GETEGID:
        return sys.unistd().getegid();
      case SYS_GETGID:
        return sys.unistd().getgid();
      case SYS_BRK:
        return sys.unistd().brk(a);
      case SYS_MMAP:
        return sys.mmap(a, b, c, d, e, f);
      case SYS_WRITEV:
        return sys.writev(a, b, c);
      case SYS_EXIT_GROUP:
        // should kill all threads.. if any
        throw new EndOfProgramException();
      case SYS_FSTAT64:
        return sys.stat().fstat64(a, b);
      case SYS_MUNMAP:
        return sys.munmap(a, b);
      case SYS_SIGACTION:
        return sys.signal().sigaction(a, b, c, 0);
      case SYS_RT_SIGACTION:
        return sys.signal().sigaction(a, b, c, d);
      case SYS_IOCTL:
        return sys.ioctl(a, b, c);
      case SYS_GETPGRP:
        return sys.unistd().getpgrp();
      case SYS_RT_SIGPROCMASK:
        return sys.signal().rt_sigprocmask(a, b, c);
      case SYS_NANOSLEEP:
        return sys.nanosleep(a, b);
      case SYS_WAIT4:
        return sys.wait_().wait4(a, b, c, d);
      case SYS_SETSID:
        return sys.unistd().setsid();
      case SYS_GETSID:
        return sys.unistd().getsid(a);
      case SYS_SETPGID:
        return sys.unistd().setpgid(a, b);
      case SYS_GETCWD:
        return sys.unistd().getcwd(a, b);
      case SYS_FCNTL64:
        return sys.fcntl().fcntl64(a, b, c);
      case SYS_GETDENTS64:
        return sys.getdents64(a, b, c);
      case SYS_DUP:
        return sys.unistd().dup(a);
      case SYS_DUP2:
        return sys.unistd().dup2(a, b);
      case SYS_SETUID:
        return sys.unistd().setuid(a);
      case SYS_LLSEEK:
        return sys.unistd().llseek(a, b, c, d, e);
      case SYS_SIGRETURN:
        // return value should be ignored
        restore();
        service();
        return 0;
      case SYS_STAT64:
        return sys.stat().stat64(a, b);
      case SYS_SOCKET:
        return sys.socket().socket(a, b, c);
      case SYS_CONNECT:
        return sys.socket().connect(a, b, c);
      case SYS_UMASK:
        return sys.stat().umask(a);
      case SYS_LSTAT:
        return sys.stat().lstat(a,b);
      case SYS_ACCESS:
        return sys.unistd().access(a,b);
      case SYS_LSTAT64:
        return sys.stat().lstat64(a,b);
      case SYS_SETSOCKOPT:
        return sys.socket().setsockopt(a, b, c, d, e);
      case SYS_NEWSELECT:
        return sys.select(a, b, c, d, e);
      case SYS_SYNC:
        return sys.unistd().sync();
      case SYS_BIND:
        return sys.socket().bind(a, b, c);
      case SYS_MOUNT:
        return sys.mount(a, b, c, d, e);
      case SYS_RENAME:
        return sys.rename(a, b);
      case SYS_READLINK:
        return sys.unistd().readlink(a, b, c);
      case SYS_SYMLINK:
        return sys.unistd().symlink(a, b);
      case SYS_RMDIR:
        return sys.unistd().rmdir(a);
      case SYS_LISTEN:
        return sys.socket().listen(a, b);
      case SYS_ACCEPT:
        return sys.socket().accept(a, b, c);
      case SYS_STATFS:
        return sys.stat().statfs(a, b);
      case SYS_FSTATFS:
        return sys.stat().fstatfs(a, b);
      case SYS_STATFS64:
        return sys.stat().statfs64(a, b, c);
      case SYS_FSTATFS64:
        return sys.stat().fstatfs64(a, b, c);
        
    }
    throw new RuntimeException("unimplemented syscall " + call);
  }
  
} // end of class Machine
