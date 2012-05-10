/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.util.*;
import cfern.*;
import cfern.cpu.*;
import cfern.mem.*;
import cfern.sys.str.*;
import cfern.fs.*;
import cfern.io.MySelector;

/**
 * A non machine specific Unix system interface, based on Linux and Posix. 
 * Provides Unix style functions open(), read(), write(), fstat(), process support, waiting, etc.
 * To implement a syscall:
 * a) get syscall number from linux kernel source, put in Syscalls (with disasm name).
 * b) write handler case in Machine.syscall().
 * c) write java implementation here.
 * Method documentation is thin here; refer to the syscall manpage or the posix specification.
 * Most public methods return 0 on success or -errno on failure.
 * TODO: needs more structure
 */
public final class SystemInterface extends Interface {
  
  /**
   * Queue of all exited processes.
   */
  private final static ExitQueue exqall = new ExitQueue();
  
  private final FileControlInterface fcntl;
  private final StatInterface stat;
  private final SignalInterface signal;
  private final WaitInterface wait;
  private final SocketInterface socket;
  private final UnistdInterface unistd;
  
  /**
   * Queue of parents exited children.
   * Null for first process.
   */
  private final ExitQueue exqparent;
  
  /**
   * Create a new system interface that operates in given memory context (since
   * most unix functions pass pointers), and with given parent pid. Opens fds
   * 0/1/2 (i.e. stdin/out/err).
   */
  public SystemInterface(Memory mem, Options opt) {
    super(mem, opt);
    fcntl = new FileControlInterface(this);
    signal = new SignalInterface(this);
    socket = new SocketInterface(this);
    stat = new StatInterface(this);
    unistd = new UnistdInterface(this);
    wait = new WaitInterface(this);
    exqparent = null;
  }
  
  /** 
   * copy a system interface as part of fork().
   * copies and increases ref count of open files.
   * parent_exq becomes exq. ppid becomes pid.
   */
  public SystemInterface(SystemInterface other, Memory mem, Options opt) {
    super(mem, opt, other);
    fcntl = new FileControlInterface(this, other.fcntl);
    signal = new SignalInterface(this, other.signal);
    socket = new SocketInterface(this);
    stat = new StatInterface(this);
    unistd = new UnistdInterface(this, other.unistd);
    wait = new WaitInterface(this);
    exqparent = other.wait.exqchild;
  }
  
  /**
   * copy this machine and return 2 different values
   */
  public int fork (Machine child) {
    // sets parent pid and parent exq
    int childpid = child.getpid();
    wait.exqchild.start(childpid);
    exqall.start(childpid);
    opt.info("fork: created child %d (%s)", childpid, child.getName());
    child.start();
    return childpid;
  }
  
  /** 
   * Get the next directory entry
   */
  public int getdents(int fd, int dirent_p, int count) {
    return getdents(fd, dirent_p, count, false);
  }
  
  /*
   * Get the next directory entry 64 bit
   */
  public int getdents64(int fd, int dirent64_p, int count) {
    return getdents(fd, dirent64_p, count, true);
  }
  
  /**
   * Get the next directory entry implementation
   */
  private int getdents(int fd, int dirent_p, int count, boolean is64) {
    FileDesc file = files.getfd(fd);
    opt.info("getdents (fd %d: %s, %s, count %d)", fd, file, mem.getname(dirent_p), count);
    if (file == null)
      return con.error(ebadf);
    if (!file.isdir())
      return con.error(enotdir);
    
    DirEnt ent = file.listnext();
    if (ent == null)
      // no more files
      return 0;
    
    ent.store(mem, dirent_p, is64);
    return ent.reclen;
  }
  
  /**
   * Get the time in seconds 
   */
  public int time(int time_p) {
    opt.info("time(%s)", mem.getname(time_p));
    if (time_p != 0 && !mem.bound(time_p))
      return con.error(efault);
    
    int t = (int) (System.currentTimeMillis() / 1000L);
    if (time_p != 0)
      mem.store_word(time_p, t);
    return t;
  }
  
  /**
   * Get number of seconds and milliseconds since epoch
   */
  public int gettimeofday(int tv_p, int tz_p) {
    opt.info("gettimeofday (%s, %s)", mem.getname(tv_p), mem.getname(tz_p));
    long t = System.currentTimeMillis();
    if (tv_p != 0) {
      Struct.store_field(mem, "timeval", "tv_sec", tv_p, (int) (t / 1000L));
      Struct.store_field(mem, "timeval", "tv_usec", tv_p, (int) ((t % 1000L) * 1000));
    }
    if (tz_p != 0) {
      opt.warn("gettimeofday: stub filling in timezone");
      // minutes west of GMT
      Struct.store_field(mem, "timezone", "tz_minuteswest", tz_p, 0);
      // daylight savings time, true or false
      Struct.store_field(mem, "timezone", "tz_dsttime", tz_p, 0);
    }
    return 0;
  }
  
  /** 
   * NOT the exit system call, call this at the end of Machine.run().
   * Notifies other processes waiting that we have exited.
   */
  public void exit(byte ret) {
    int open = files.open();
    if (open > 0)
      opt.warn("exit: %d files left open", open);
    files.closeall(false);
    
    ExitValue ex = new ExitValue(unistd().getpid(), ret);
    // may cause exception if parent has exited.
    if (exqparent != null)
      exqparent.exit(ex);
    exqall.exit(ex);
    Machines.signal(unistd().getppid(), con.get("SIGCHLD"));
  }
  
  /**
   * Get the host name, kernel version etc of the current machine
   */
  public int uname(int uts_p) {
    opt.debug("uname (uts %s)", mem.getname(uts_p));
    if (!mem.bound(uts_p))
      return con.error(efault);
    Struct.store_field(mem, "utsname", "sysname", uts_p, "Linux"); // yeah right
    Struct.store_field(mem, "utsname", "nodename", uts_p, "mips");
    Struct.store_field(mem, "utsname", "release", uts_p, "2.6.18");
    Struct.store_field(mem, "utsname", "version", uts_p, "Cfern 0.2");
    Struct.store_field(mem, "utsname", "machine", uts_p, "MIPS R2000");
    // return this and glibc parses /proc/sys/kernel/osrelease manually
    // return -con.get("ENOSYS");
    return 0;
  }
  
  /**
   * Map a file at start_p (may be null).
   * On munmap and msync, if file is writeable then flush to disk.
   * MAP_ANONYMOUS (with fd=-1) means don't map anything, just return a blank page.
   * Prot is ignored (all pages are r/w/x).
   * void *mmap(void *start, size_t length, int prot, int flags, int fd, off_t offset);
   */
  public int mmap(int start_p, int len, int prot, int flags, int fd, int off) {
    FileDesc file = files.getfd(fd, false);
    opt.info("mmap(%s, len %d, %s, %s, fd %d: %s, off %d)", 
        mem.getname(start_p), len, con.names("PROT_", prot), con.names("MAP_", flags), fd, file, off);
    // mem must be writeable, and if shared, must be written back to file on close
    boolean write = con.has(prot, "PROT_WRITE");
    // map nothing
    boolean anon = con.has(flags, "MAP_ANONYMOUS");
    // copy on write. does not write back.
    boolean priv = con.has(flags, "MAP_PRIVATE");
    // shared between processes
    boolean share = con.has(flags, "MAP_SHARED");
    
    if ((priv && share) || !(priv || share)) {
      // error to not be priv or shared
      opt.error("mmap: bad flags: %s", con.names("MAP_", flags));
      return con.error(einvalid);
    }
    
    if (len == 0)
      //EINVAL (since Linux 2.6.12), length was 0. (from mmap man page)
      return con.error(einvalid);
    
    if (anon) {
      // ignore fd, just map /dev/zero
      if (start_p == 0)
        return mem.alloc(0, len);
      throw new RuntimeException("mmap of fixed area");
    }
    
    if (file == null)
      return con.error(ebadf);
    
    // file access
    String err = null;
    if (!file.isreg()) {
      err = "not a regular file";
    } else if (!file.readable()) {
      err = "file not readable";
    } else if (share && write && !file.writeable()) {
      err = "file not writable for shared mapping";
    }
    if (err != null) {
      opt.error("mmap: %s: %s", err, file);
      return con.error(eaccess);
    }
    
    return mem.map(file, off, len, share);
  }
  
  /**
   * unmap the given region
   * FIXME may need to write back to file
   */
  public int munmap(int start_p, int len) {
    opt.info("munmap (%s, len %d)", mem.getname(start_p), len);
    mem.free(start_p);
    opt.info("munmap: stub not writing back");
    return 0;
  }
  
  /**
   * Write data into multiple buffers
   */
  public int writev(int fd, int iovec_p, int count) {
    FileDesc file = files.getfd(fd);
    opt.info("writev(fd %d: %s, iovec %s, count %d)", fd, file, mem.getname(iovec_p), count);
    if (file == null)
      return con.error(ebadf);
    
    int size = con.get("sizeof_iovec");
    int ret = 0;
    for (int n = 0; n < count; n++) {
      int vec_p = iovec_p + (n * size);
      int base_p = Struct.load_field(mem, "iovec", "iov_base", vec_p);
      int len = Struct.load_field(mem, "iovec", "iov_len", vec_p);
      int wret = unistd().write(fd, base_p, len);
      if (wret < 0)
        return wret;
      ret += wret;
    }
    return ret;
  }
  
  /**
   * Device and terminal control.
   * See man iotcl_list for numbers.
   * Also linux/include/asm-mips/ioctls.h
   * TODO probably needs to delegate to Terminal instance
   */
  public int ioctl(int fd, int arg, int arg_p) {
    FileDesc file = files.getfd(fd);
    String name = con.namerx("TC|FIO|TIO", arg);
    opt.info("ioctl (fd %d: %s, arg %s, argp %s)", fd, file, name, mem.getname(arg_p));
    if (file == null)
      return con.error(ebadf);
    
    if (con.is(arg, "TCSETS")) {
      if (file.getTerminal() == null) {
        opt.warn("ioctl: tcsets: not a tty");
        return con.error(enottty);
      }
      Termios termios = file.getTerminal().getTermios();
      
      String old = termios.toString();
      termios.load(mem, arg_p);
      opt.info("ioctl: tcsets: was %s is now %s", old, termios);
      return 0;
    }
    
    if (con.is(arg, "TCGETS")) {
      // arg_p is a termios struct.. glibc __isatty only checks return value
      // see glibc-2.3.6/sysdeps/posix/isatty.c
      if (file.getTerminal() == null)
        return con.error(enottty);
      Termios termios = file.getTerminal().getTermios();
      
      opt.info("ioctl: tcgets: storing %s", termios);
      termios.store(mem, arg_p);
      return 0;
    }
    
    if (con.is(arg, "TCSETSW")) {
      // arg_p is termios
      // same as TCSADRAIN - change when pending output is written
      // glibc-2.3.6/sysdeps/unix/sysv/linux/mips/bits/termios.h
      opt.info("ioctl(TCSETSW): stub not doing anything");
      return 0;
    }
    
    if (con.is(arg, "TIOCGWINSZ")) {
      // terminal window size
      // linux/drivers/char/tty_io.c
      if (!mem.bound(arg_p))
        return con.error(efault);
      Winsize win = new Winsize().init();
      opt.info("ioctl: storing window size %s", win);
      win.store(mem, arg_p);
      return 0;
    }
    
    if (con.is(arg, "TIOCSWINSZ")) {
      // see linux/drivers/char/tty_io.c#L2945
      // static int tiocswinsz(struct tty_struct *tty, struct tty_struct *real_tty, struct winsize __user * arg)
      if (!mem.bound(arg_p))
        return con.error(efault);
      Winsize win = new Winsize().load(mem, arg_p);
      opt.info("ioctl: loaded window size %s", win);
      // don't do anything with it...
      return 0;
    }
    
    if (con.is(arg, "TIOCGPGRP")) {
      // get pid of program controlling terminal
      // glibc/sysdeps/unix/bsd/tcgetpgrp.c
      if (!mem.bound(arg_p))
        return con.error(efault);
      opt.info("* tcgetpgrp: everyone's a winner");
      // TODO this should probably come from terminal not current pgrp
      mem.store_word(arg_p, unistd().getpgrp());
      return 0;
    }
    
    if (con.is(arg, "TIOCSPGRP")) {
      // set terminal program group (man tcsetpgrp)
      opt.info("tcsetpgrp: stub not doing anything");
      return 0;
    }
    
    if (con.is(arg, "TIOCGPTN")) {
      // get pty number of terminal
      // TODO really get pty number of terminal
      mem.store_word(arg_p, file.getTerminal().num());
      return 0;
    }
    
    if (con.is(arg, "TIOCSPTLCK")) {
      // lock or unlock pty
      opt.warn("ignoring ptlock %d", mem.load_word(arg_p));
      // tty.unlock() ...
      return 0;
    }
    
    // throw an exception to get the user backtrace
    throw new RuntimeException("ioctl " + name + " not done");
  }
  
  public int nanosleep(int tsreq_p, int tsrem_p) {
    opt.debug("nanosleep (req %s, rem %s)", mem.getname(tsreq_p), mem.getname(tsrem_p));
    
    int sec = Struct.load_field(mem, "timespec", "tv_sec", tsreq_p);
    int nsec = Struct.load_field(mem, "timespec", "tv_nsec", tsreq_p);
    if (sec < 0 || nsec > 999999999)
      return con.error(einvalid);
    // sleep at least 1ms
    if (sec == 0 && nsec < 1000000)
      return 0;
    
    long sleep = sec * 1000L + (nsec / 1000000);
    opt.trace("nanosleep for %s ms", sleep);
    // should probably use nanoTime instead
    long start = System.currentTimeMillis();
    
    try {
      Thread.sleep(sec * 1000L + (nsec / 1000000));
      return 0;
      
    } catch (InterruptedException e) {
      // interrupted by signal
      // fill in rem struct
      if (tsrem_p != 0) {
        long rem = sleep - (System.currentTimeMillis() - start);
        // avoid infinite loop of nanosleep
        if (rem < 0 || rem >= sleep)
          rem = 0;
        opt.warn("nanosleep: interrupted, %s ms remaining", rem);
        Struct.store_field(mem, "timespec", "tv_sec", tsrem_p, (int) (rem / 1000L));
        Struct.store_field(mem, "timespec", "tv_nsec", tsrem_p, (int) ((rem % 1000) * 1000));
      }
      return con.error(eintr);
    }
  }
  
  /**
   * Select.
   * See fd_set definition in linux/include/linux/posix_types.h
   * Also see man select_tut.
   */
  public int select(int maxfd, int readfds_p, int writefds_p, int exceptfds_p, int timeout_p) {
    int len = (maxfd + 8) / 8;
    short[] rd = (readfds_p != 0) ? mem.load_bitset(readfds_p, len) : null;
    short[] wr = (writefds_p != 0) ? mem.load_bitset(writefds_p, len) : null;
    short[] ex = (exceptfds_p != 0) ? mem.load_bitset(exceptfds_p, len) : null;
    FileDesc[] rdf = files.getfds(rd), wrf = files.getfds(wr), exf = files.getfds(ex);
    opt.info("select (max %d, read %s: %s, write %s: %s, except %s: %s, time %s)",
        maxfd, Arrays.toString(rd), Arrays.toString(rdf), Arrays.toString(wr), 
        Arrays.toString(wrf), Arrays.toString(ex), Arrays.toString(exf), mem.getname(timeout_p));
    
    if (maxfd < 0)
      return con.error(einvalid);
    if (ex != null)
      throw new RuntimeException("except selects unimplemented");
    if (timeout_p != 0)
      throw new RuntimeException("timeout selects unimplemented");
    
    /*
    if (rd == null) {
      opt.warn("select: nothing to select from\n");
      return 0;
    }
    */
    
    // check files are open
    if (Driver.indexOfSafe(rdf, null) >= 0) {
      opt.error("select: bad read file set %s", Arrays.toString(rdf));
      return con.error(ebadf);
    }
    if (Driver.indexOfSafe(wrf, null) >= 0) {
      opt.error("select: bad write file set %s", Arrays.toString(wrf));
      return con.error(ebadf);
    }
    
    MySelector sel = MySelector.newInstance(rdf, rd, wrf, wr);
    if (sel == null) {
      opt.warn("select: nothing to select from");
      return 0;
    }
    short[][] fds = sel.select();
    int ret = 0;
    if (readfds_p != 0)
      ret += mem.store_bitset(readfds_p, len, fds[0]);
    if (writefds_p != 0)
      ret += mem.store_bitset(writefds_p, len, fds[1]);
    return ret;
  }
  
  /**
   * Mount filesystem fs as tgt using device src and options data.
   * Note: device doesn't really exist, e.g. it may be a native path
   */
  public int mount(int src_p, int tgt_p, int fs_p, int flags, int data_p) {
    String src = mem.load_string(src_p);
    String tgt = mem.load_string(tgt_p);
    String fs = mem.load_string(fs_p);
    // always seems to be null for some reason
    String data = mem.load_string(data_p);
    opt.info("mount(%s,%s,%s,%d,%s)", src, tgt, fs, flags, data);
    if (src == null || tgt == null || fs == null)
      return con.error(efault);
    
    // TODO check tgt exists and is dir
    boolean ret = FileSystem.get().mount(fs, src, tgt);
    return ret ? 0 : con.error(enodev);
  }
  
  /**
   * Rename file (from and to must be same file system)
   */
  public int rename(int from_p, int to_p) {
    String from = mem.load_string(from_p);
    String to = mem.load_string(to_p);
    opt.info("rename(%s,%s)", from, to);
    if (from == null || to == null)
      return con.error(efault);
    
    String ret = FileSystem.get().rename(files.abspath(from), files.abspath(to));
    return con.error(ret);
  }
  

  public FileControlInterface fcntl() {
    return fcntl;
  }
  

  public StatInterface stat() {
    return stat;
  }
  

  public SignalInterface signal() {
    return signal;
  }
  

  public WaitInterface wait_() {
    return wait;
  }
  


  public SocketInterface socket() {
    return socket;
  }
  

  public UnistdInterface unistd() {
    return unistd;
  }
    
  
} // end of class SystemInterface ^_^
