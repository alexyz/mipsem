/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;
import java.util.Arrays;

/**
 * System call constants
 */
public final class Syscalls {
  
  public static final Name[] sys_names;
  private static final Name undef;
  
  /**
   * Initialise the disassembly names
   */
  static {
    undef = new Name("syscall?");
    sys_names = new Name[320];
    init();
  }
  
  private Syscalls() {
    // private to prevent javadoc
  }
  
  /**
   * Get syscall name (as sys may be out of bounds)
   */
  public static Name sys_name(int sys) {
    return sys >= 0 && sys < sys_names.length ? sys_names[sys] : undef;
  }
  
  /**
   * Syscall numbers from linux/include/asm-mips/unistd.h
   */
  public static final int
  SYS_SYSCALL = 0,
  SYS_EXIT = 1,
  SYS_FORK = 2,
  SYS_READ = 3,
  SYS_WRITE = 4,
  SYS_OPEN = 5,
  SYS_CLOSE = 6,
  SYS_WAITPID = 7,
  SYS_CREAT = 8,
  SYS_LINK = 9,
  SYS_UNLINK = 10,
  SYS_EXECVE = 11,
  SYS_CHDIR = 12,
  SYS_TIME = 13,
  SYS_CHMOD = 15,
  SYS_LSEEK = 19,
  SYS_GETPID = 20,
  SYS_MOUNT = 21,
  SYS_SETUID = 23,
  SYS_GETUID = 24,
  SYS_ACCESS = 33,
  SYS_SYNC = 36,
  SYS_KILL = 37,
  SYS_RENAME = 38,
  SYS_MKDIR = 39,
  SYS_RMDIR = 40,
  SYS_DUP = 41,
  SYS_PIPE = 42,
  SYS_BRK = 45,
  SYS_GETGID = 47,
  SYS_GETEUID = 49,
  SYS_GETEGID = 50,
  SYS_IOCTL = 54,
  SYS_FCNTL = 55,
  SYS_SIGNAL = 56,
  SYS_SETPGID = 57,
  SYS_UMASK = 60,
  SYS_DUP2 = 63,
  SYS_GETPPID = 64,
  SYS_GETPGRP = 65,
  SYS_SETSID = 66,
  SYS_SIGACTION = 67,
  SYS_GETTIMEOFDAY = 78,
  SYS_SYMLINK = 83,
  SYS_READLINK = 85,
  SYS_MMAP = 90,
  SYS_MUNMAP = 91,
  SYS_STATFS = 99,
  SYS_FSTATFS = 100,
  SYS_STAT = 106,
  SYS_LSTAT = 107,
  SYS_FSTAT = 108,
  SYS_WAIT4 = 114,
  SYS_SIGRETURN = 119,
  SYS_UNAME = 122,
  SYS_LLSEEK = 140,
  SYS_GETDENTS = 141,
  SYS_NEWSELECT = 142,
  SYS_WRITEV = 146,
  SYS_GETSID = 151,
  SYS_NANOSLEEP = 166,
  SYS_BIND = 169,
  SYS_ACCEPT = 168,
  SYS_CONNECT = 170,
  SYS_LISTEN = 174,
  SYS_SETSOCKOPT = 181,
  SYS_SOCKET = 183,
  SYS_RT_SIGACTION = 194,
  SYS_RT_SIGPROCMASK = 195,
  SYS_GETCWD = 203,
  SYS_STAT64 = 213,
  SYS_LSTAT64 = 214,
  SYS_FSTAT64 = 215,
  SYS_GETDENTS64 = 219,
  SYS_FCNTL64 = 220,
  SYS_EXIT_GROUP = 246,
  SYS_STATFS64 = 255,
  SYS_FSTATFS64 = 256;
  
  static void init() {
    Arrays.fill(sys_names, undef);
    
    // added for newlib
    // glibc encodes this one only and puts service in reg 2
    sys_names[SYS_SYSCALL] = new Name("syscall");
    sys_names[SYS_EXIT] = new Name("exit");
    sys_names[SYS_FORK] = new Name("fork");
    sys_names[SYS_READ] = new Name("read");
    sys_names[SYS_WRITE] = new Name("write");
    sys_names[SYS_OPEN] = new Name("open");
    sys_names[SYS_CLOSE] = new Name("close");
    sys_names[SYS_WAITPID] = new Name("waitpid");
    sys_names[SYS_CREAT] = new Name("creat");
    sys_names[SYS_LINK] = new Name("link");
    sys_names[SYS_UNLINK] = new Name("unlink");
    sys_names[SYS_EXECVE] = new Name("execve");
    sys_names[SYS_CHDIR] = new Name("chdir");
    sys_names[SYS_TIME] = new Name("time");
    sys_names[SYS_CHMOD] = new Name("chmod");
    sys_names[SYS_STAT] = new Name("stat");
    sys_names[SYS_LSEEK] = new Name("lseek");
    sys_names[SYS_GETPID] = new Name("getpid");
    sys_names[SYS_PIPE] = new Name("pipe");
    sys_names[SYS_GETUID] = new Name("getuid");
    sys_names[SYS_FSTAT] = new Name("fstat");
    sys_names[SYS_FCNTL] = new Name("fnctl");
    sys_names[SYS_GETPPID] = new Name("getppid");
    sys_names[SYS_GETDENTS] = new Name("getdents");
    sys_names[SYS_KILL] = new Name("kill");
    sys_names[SYS_GETTIMEOFDAY] = new Name("gettimeofday");
    sys_names[SYS_SIGNAL] = new Name("signal");
    
    // added for glibc
    sys_names[SYS_UNAME] = new Name("uname");
    sys_names[SYS_GETEUID] = new Name("geteuid");
    sys_names[SYS_GETEGID] = new Name("getegid");
    sys_names[SYS_GETGID] = new Name("getgid");
    sys_names[SYS_BRK] = new Name("brk");
    sys_names[SYS_MMAP] = new Name("mmap");
    sys_names[SYS_WRITEV] = new Name("writev");
    sys_names[SYS_EXIT_GROUP] = new Name("exit_group");
    sys_names[SYS_FSTAT64] = new Name("fstat64");
    sys_names[SYS_MUNMAP] = new Name("munmap");
    sys_names[SYS_SIGACTION] = new Name("sigaction");
    sys_names[SYS_RT_SIGACTION] = new Name("rt_sigaction");
    sys_names[SYS_IOCTL] = new Name("ioctl");
    sys_names[SYS_GETPGRP] = new Name("getpgrp");
    sys_names[SYS_RT_SIGPROCMASK] = new Name("rt_sigprocmask");
    sys_names[SYS_NANOSLEEP] = new Name("nanosleep");
    sys_names[SYS_WAIT4] = new Name("wait4");
    sys_names[SYS_SETSID] = new Name("setsid");
    sys_names[SYS_GETSID] = new Name("getsid");
    sys_names[SYS_SETPGID] = new Name("setpgid");
    sys_names[SYS_GETCWD] = new Name("getcwd");
    sys_names[SYS_FCNTL64] = new Name("fcntl64");
    sys_names[SYS_GETDENTS64] = new Name("getdents64");
    sys_names[SYS_DUP] = new Name("dup");
    sys_names[SYS_DUP2] = new Name("dup2");
    sys_names[SYS_SETUID] = new Name("setuid");
    sys_names[SYS_LLSEEK] = new Name("llseek");
    sys_names[SYS_SIGRETURN] = new Name("sigreturn");
    sys_names[SYS_STAT64] = new Name("stat64");
    sys_names[SYS_SOCKET] = new Name("socket");
    sys_names[SYS_CONNECT] = new Name("connect");
    sys_names[SYS_SETSOCKOPT] = new Name("setsockopt");
    sys_names[SYS_UMASK] = new Name("umask");
    sys_names[SYS_LSTAT] = new Name("lstat");
    sys_names[SYS_LSTAT64] = new Name("lstat64");
    sys_names[SYS_NEWSELECT] = new Name("select");
    sys_names[SYS_SYNC] = new Name("sync");
    sys_names[SYS_BIND] = new Name("bind");
    sys_names[SYS_MOUNT] = new Name("mount");
    sys_names[SYS_RENAME] = new Name("rename");
    sys_names[SYS_READLINK] = new Name("readlink");
    sys_names[SYS_SYMLINK] = new Name("symlink");
    sys_names[SYS_RMDIR] = new Name("rmdir");
    sys_names[SYS_LISTEN] = new Name("listen");
    sys_names[SYS_ACCEPT] = new Name("accept");
    sys_names[SYS_STATFS] = new Name("statfs");
    sys_names[SYS_FSTATFS] = new Name("fstatfs");
    sys_names[SYS_STATFS64] = new Name("statfs64");
    sys_names[SYS_FSTATFS64] = new Name("fstatfs64");
  }
  
} // end of class Syscalls
