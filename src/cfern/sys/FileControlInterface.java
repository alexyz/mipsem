/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.io.IOException;

import cfern.fs.*;

/**
 * Represents functions in <fcntl.h>.
 * Also maintains terminal.
 */
public class FileControlInterface extends Interface {
  
  /**
   * Current terminal. Note that any file any have a terminal, accessed though
   * FileDesc.getTerminal().
   */
  private Terminal tty;
  
  public FileControlInterface(SystemInterface sys) {
    super(sys);
  }
  
  public FileControlInterface(SystemInterface sys, FileControlInterface other) {
    super(sys);
    tty = other.tty;
  }
  
  /**
   * File control
   */
  public int fcntl(int fd, int cmd, int arg) {
    return fcntl(fd, cmd, arg, false);
  }
  
  /**
   * File control 64
   */
  public int fcntl64(int fd, int cmd, int arg) {
    return fcntl(fd, cmd, arg, true);
  }
  
  /**
   * File control
   */
  private int fcntl(int fd, int cmd, int arg, boolean is64) {
    FileDesc file = files.getfd(fd);
    opt.info("fcntl (fd %d: %s, cmd %s, %d)", fd, file, con.name("F_", cmd), arg);
    if (file == null)
      return con.error(ebadf);
    
    int ret;
    
    if (con.is(cmd, "F_DUPFD")) {
      // duplicate file handle
      ret = files.newfd(file.dup());
      opt.info("fcntl F_DUPFD: %d -> %d", fd, ret);
      
    } else if (con.is(cmd, "F_GETFD")) {
      // get close on exec
      // kind of a guess...
      ret = files.getCloseOnExec(fd) ? 1 : 0;
      
    } else if (con.is(cmd, "F_SETFD")) {
      // set close on exec
      boolean cl = con.is(arg, "FD_CLOEXEC");
      files.setCloseOnExec(fd, cl);
      ret = 0;
      
    } else if (con.is(cmd, "F_GETFL")) {
      // return file status flags
      ret = 0;
      if (file.isNonBlock())
        ret |= con.get("O_NONBLOCK");
      
    } else if (con.is(cmd, "F_SETFL")) {
      // set file status flags (see open())
      boolean nonBlock = con.has(arg, "O_NONBLOCK");
      if (nonBlock && !file.isNonBlock())
        opt.warn("fcntl: setting %s non block", file);
      file.setNonBlock(nonBlock);
      ret = 0;

    } else {
      throw new RuntimeException("fcntl cmd " + cmd + " unimplemented");
    }
    
    return ret; 
  }
  
  /**
   * Open a file with various options flags and access mode (if creating)
   */
  public int open (int path_p, int flags, int mode) {
    String path = mem.load_string(path_p);
    opt.info("open (%s, %s, %x)", path, con.names("O_", flags), mode);
    
    // need to check for empty string before prefixing cwd
    if (path.length() == 0)
      return con.error(enoent);
    
    // access mode. note O_RDONLY is 0
    boolean
      rdonly = con.is(flags, "O_RDONLY"),
      wronly = con.has(flags, "O_WRONLY"),
      rdwr = con.has(flags, "O_RDWR"),
      rd = rdonly || rdwr,
      wr = wronly || rdwr;
    
    // creation flags
    boolean
      cr = con.has(flags, "O_CREAT"),
      ex = con.has(flags, "O_EXCL"),
      not = con.has(flags, "O_NOCTTY"),
      tr = con.has(flags, "O_TRUNC");
    
    // status flags
    // ignores O_DIRECTORY, O_DIRECT, O_LARGEFILE, NOATIME, O_NOFOLLOW, O_SYNC
    boolean
      ap = con.has(flags, "O_APPEND"),
      as = con.has(flags, "O_ASYNC"),
      nonb = con.has(flags, "O_NONBLOCK");
    
    if (nonb)
      opt.warn("open: non blocking unimplemented");
    if (as)
      throw new RuntimeException("open: async unimplemented");
    
    FileDesc[] fd = new FileDesc[1];
    String ret = FileSystem.get().open(fd, files.abspath(path), rd, wr, cr, ex, ap, tr);
    if (ret != null) {
      opt.warn("open: error opening %s: %s", path, ret);
      return con.error(ret);
    }
    
    if (nonb)
      fd[0].setNonBlock(true);
    
    // replace terminal
    Terminal newtty = fd[0].getTerminal();
    if (!not && newtty != null && newtty != tty) {
      opt.warn("open: should replace terminal %s with %s", tty, newtty);
      //tty = newtty;
    }
    return files.newfd(fd[0]);
  }
  
  /**
   * Get the current terminal
   */
  public Terminal getTerminal() {
    return tty;
  }
  
  /**
   * Open the given file as fd 0, 1 and 2.
   * NOT a system call, throws RuntimeException on error.
   */
  public void openTerminal(String path) {
    FileDesc f;
    try {
      f = FileSystem.get().openex(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (f.getTerminal() == null)
      throw new RuntimeException("not a terminal: " + path + ": " + f);
    files.replacefd(0, f);
    files.replacefd(1, f);
    files.replacefd(2, f);
    this.tty = f.getTerminal();
  }
  
}
