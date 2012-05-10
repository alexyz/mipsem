/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.io.IOException;
import java.util.ArrayList;
import cfern.cpu.*;
import cfern.elf.ElfLoader;
import cfern.fs.*;
import cfern.sys.str.Stat;

/**
 * Base class for system interface implementations
 */
public class UnistdInterface extends Interface {
  
  /** process id, parent process id (constant) */
  private final int pid, ppid;
  
  /** program group, session (may change) */
  private int pgrp, sid;
  
  public UnistdInterface(SystemInterface sys) {
    super(sys);
    pid = Machines.newpid();
    pgrp = 0; 
    ppid = 0;
  }
  
  public UnistdInterface(SystemInterface sys, UnistdInterface uni) {
    super(sys);
    pid = Machines.newpid();
    pgrp = uni.pgrp;
    ppid = uni.getpid();
    sid = uni.sid;
  }
  
  /**
   * Check file existence/access
   */
  public int access (int path_p, int mode) {
    String path = mem.load_string(path_p);
    opt.info("access (%s, %o)", path, mode);
    String ret = FileSystem.get().access(files.abspath(path), mode);
    return con.error(ret);
  }
  
  /**
   * Set the end of the data segment, update brk
   */
  public int brk(int end_p) {
    opt.info("brk(%s)", mem.getname(end_p));
    return mem.brk(end_p);
  }
  
  /**
   * Change current working directory.
   */
  public int chdir(int path_p) {
    String path = mem.load_string(path_p);
    opt.info("chdir(%s)", path);
    if (path.length() == 0)
      return con.error(enoent);
    
    // need trailing / otherwise it might look like a file
    // also need reduced path for cwd
    path = FileSystemUtil.reduce(files.abspath(path + "/"));
    
    Stat[] st = new Stat[1];
    String ret = FileSystem.get().stat(st, path, false);
    if (ret != null)
      return con.error(ret);
    if (!con.has(st[0].mode, Stat.dir))
      return con.error(enotdir);
    
    opt.info("chdir: cwd %s -> %s ", files.getcwd(), path);
    files.setcwd(path);
    return 0;
  }
  
  /**
   * Close the given file. Allow file number to be reused
   */
  public int close (int fd) {
    FileDesc file = files.getfd(fd);
    opt.info("close (%d: %s)", fd, file);
    if (file == null)
      return con.error(ebadf);
    
    files.close(fd);
    return 0;
  }
  
  /**
   * Duplicate file handle. Close on exec is not kept.
   */
  public int dup(int fd) {
    FileDesc file = files.getfd(fd);
    opt.info("dup(%d: %s)", fd, file);
    if (file == null)
      return con.error(ebadf);
    
    int ret = files.newfd(file.dup());
    opt.debug("dup = %d", fd, ret);
    return ret;
  }
  
  /**
   * Close newfd if open, then duplicate fd in it's place.
   */
  public int dup2(int fd, int newfd) {
    FileDesc file = files.getfd(fd);
    opt.info("dup2 (%d: %s, %d)", fd, file, newfd);
    if (file == null)
      return con.error(ebadf);
    
    files.replacefd(newfd,file);
    return newfd;
  }
  
  
  /**
   * Load a new program into current machine. Copy args and env.
   */
  public int execve(Machine mach, int path_p, int arg_pp, int env_pp) {
    String path = mem.load_string(path_p);
    ArrayList<String> args = mem.load_env(arg_pp);
    ArrayList<String> env = mem.load_env(env_pp);
    
    opt.info("execve(%s,%s,%s)", path, mem.getname(arg_pp), mem.getname(env_pp));
    
    try {
      String apath = files.abspath(path);
      FileDesc uf = FileSystem.get().openex(apath);
      if (!uf.isreg()) {
        opt.warn("execve: not regular file: %s", apath);
        return con.error(enoent);
      }
        
      ElfLoader elf = ElfLoader.loadelf(uf);
      
      // commit to loading program
      // close files
      files.closeall(true);
      mach.load(elf, args, env);
      return 0;
      
    } catch (IOException e) {
      // probably not found
      opt.warn("execve: error loading %s: %s", path, e.toString());
      return con.error(enoent);
    }
  }
  
  /**
   * Write bytes/string to any open file
   */
  public int write (int fd, int buf_p, int len) {
    FileDesc file = files.getfd(fd);
    if (opt.info)
      opt.info("write (%d: %s, %s, %d)", fd, file, mem.getname(buf_p), len);  
    if (file == null || !file.writeable())
      return con.error(ebadf);
    
    int ret = mem.write_to(file, buf_p, len);
    if (ret < len)
      throw new RuntimeException("could not write " + len + " bytes");
    
    return ret;
  }
  
  /**
   * Get current working directory.
   */
  public int getcwd(int buf_p, int len) {
    String cwd = files.getcwd();
    if (cwd.length() > 1)
      cwd = cwd.substring(0, cwd.length() - 1);
    opt.info("getcwd (%s, len %d) = %s", mem.getname(buf_p), len, cwd);
    
    if (len == 0)
      return con.error(einvalid);
    if (cwd.length() + 1 >= len)
      return con.error(erange);
    mem.store_string(buf_p, cwd);
    return 0;
  }
  
  /**
   * Get effective group id
   */
  public int getegid() {
    opt.debug("getegid");
    return 0;
  }
  
  /**
   * Get effective user id
   */
  public int geteuid() {
    opt.debug("geteuid");
    return 0;
  }
  
  /**
   * Get actual group id
   */
  public int getgid() {
    opt.debug("getgid");
    return 0;
  }
  
  public int getpgid(int process) {
    throw new RuntimeException("getpgid unimp");
  }
  
  /**
   * get the program group of this process.
   * this can also be done through ioctl TIOCGPGRP.
   */
  public int getpgrp() {
    opt.debug("getpgrp");
    return pgrp;
  }
  
  /** get process number */
  public int getpid() {
    return pid;
  }
  
  /** get parent process number */
  public int getppid() {
    return ppid;
  }
  
  /**
   * Get the session id, whatever that is
   */
  public int getsid(int process) {
    opt.info("getsid(%d) = %d", process, sid);
    if (process != 0)
      throw new RuntimeException("cannot get sid of pid " + process);
    return sid;
  }
  
  /**
   * Get actual user id
   */
  public int getuid() {
    opt.debug("getuid");
    return 0;
  }
  
  /**
   * Make a hard file link.
   */
  public int link(int from_p, int to_p) {
    // java doesnt do hard links so copy instead
    // get file names  
    String frompath = mem.load_string(from_p), topath = mem.load_string(to_p);
    opt.warn("link: copying %s to %s", frompath, topath);
    throw new RuntimeException("link not done");
  }
  
  /**
   * Seek to position in file. Delegates to lseek.
   */
  public int llseek (int fd, int high, int low, int res_p, int whence) {
    opt.warn("llseek (fd %d, pos %d:%d, res %s, %s)", 
        fd, high, low, mem.getname(res_p), con.name("SEEK_", whence));
    int res = lseek(fd, low, whence);
    if (res >= 0) {
      mem.store_word(res_p, res);
      return 0;
    }
    // error
    return res;
  }
  
  /**
   * Seek to position in file or directory.
   * Returns offset from start of file.
   */
  public int lseek (int fd, int offset, int whence) {
    FileDesc file = files.getfd(fd);
    opt.info("lseek (fd %d: %s, off %d, %s)", fd, file, offset, con.name("SEEK_", whence));
    if (file == null)
      return con.error(ebadf);
    if (!file.seekable())
      return con.error(eispipe); // check
    
    if (whence == con.get("SEEK_SET"))
      return file.seekset(offset);
    else if (whence == con.get("SEEK_CUR"))
      return file.seekcur(offset);
    else if (whence == con.get("SEEK_END"))
      return file.seekend(offset);
    else
      return con.error(einvalid);
  }
  
  /**
   * Create streams that read and write to each other, returns fds in fd_p[0]
   * and fd_p[1]. Note: Linux kernel actually returns fd[0] as result and fd[1]
   * in arch-specific way, on mips in reg[3]. This is handled in Mips.syscall.
   */
  public int pipe(int fd_p) {
    opt.info("pipe (%s)", mem.getname(fd_p));
    if (!mem.bound(fd_p))
      return con.error(efault);

    FileDesc[] uf = FileSystemUtil.pipe();
    int rd = files.newfd(uf[0]);
    mem.store_word(fd_p, rd);
    int wr = files.newfd(uf[1]);
    mem.store_word(fd_p + 4, wr);

    opt.debug("pipe: created rd=%d wr=%d", rd, wr);
    return 0;
  }
  
  /**
   * Read from file.
   * Return number of bytes read, 0 on eof, -1 on error.
   */
  public int read (int fd, int buf_p, int len) {
    FileDesc file = files.getfd(fd);
    if (opt.info)
      opt.info("read (%d: %s, %s, %d)", fd, file, mem.getname(buf_p), len);
    if (file == null || !file.readable())
      return con.error(ebadf);
    
    if (file.isNonBlock() && file.inChannel() == null && file.available() == 0)
      throw new RuntimeException("read: blocking read from non block file " + file);
    
    int ret = mem.read_from(file, buf_p, len); // will be 0 on EOF, -1 on err
    
    // would be nice to print string representation of buffer
    if (opt.info)
      opt.info("read: read %d bytes", ret);
    return ret;
  }
  
  /**
   * Read path of symlink
   */
  public int readlink(int path_p, int buf_p, int len) {
    //if (!mem.bound(path_p) || !mem.bound(buf_p))
      //return con.error(efault);
    
    String path = mem.load_string(path_p);
    opt.info("readlink(%s,%s,%d)", path, mem.getname(buf_p), len);
    
    if (len < 0)
      return con.error(einvalid);
    if (path == null || !mem.bound(buf_p))
      return con.error(efault);
    
    String[] targetret = new String[1];
    String err = FileSystem.get().readlink(targetret, files.abspath(path));
    if (err != null)
      return con.error(err);
    
    String target = targetret[0];
    if (target.length() > (len - 1))
      target = target.substring(0, (len - 1));
    //opt.info("readlink: target is %s", target);
    mem.store_string(buf_p, target);
    return target.length();
  }
  
  /**
   * Be rid of directory 
   */
  public int rmdir(int path_p) {
    String path = mem.load_string(path_p);
    opt.info("rmdir(%s)", path);
    if (path == null)
      return con.error(efault);
    if (path.length() == 0)
      return con.error(enoent);
    
    String err = FileSystem.get().rmdir(files.abspath(path));
    return con.error(err);
  }
  
  /**
   * setpgid() sets the process group ID of the process specified by pid to
   * pgid. 
   * If pid is zero, the process ID of the current process is used. 
   * If pgid is zero, the process ID of the process specified by pid is used.
   */
  public int setpgid(int pid, int pgid) {
    opt.info("setpgid (pid %d, pg %d)", pid, pgid);
    // if pid is zero use current pid
    if (pid != 0) {
      opt.info("setpgid: stub ignoring set pgrp of other process");
      return 0;
    }
    if (pid == 0)
      pid = this.pid;
    // if pgid is 0 use pid
    if (pgid == 0)
      pgid = pid;
    pgrp = pgid;
    return 0;
  }
  
  /**
   * Set the program group to the current pid
   * This should check something
   */
  public int setpgrp() {
    return setpgid(0, 0);
  }
  
  /**
   * Set the session id equal to the current pid
   */
  public int setsid() {
    opt.info("setsid() %d -> %d", sid, pid);
    // perhaps should return EPERM if sid already equals pid
    sid = pid;
    return sid;
  }
  
  /**
   * Set user id. For some reason mpg123 likes to call this
   */
  public int setuid(int id) {
    opt.info("setuid %d ignored", id);
    return 0;
  }
  
  /**
   * Pause execution for specified number of seconds
   */
  public int sleep(int len) {
    opt.warn("sleep(%d)", len);
    try {
      Thread.sleep(len * 1000L);
      return 0;
    } catch (InterruptedException e) {
      // received signal, services after syscall return
      opt.warn("sleep: interrupted");
      return con.error(eintr);
    }
  }
  
  /**
   * Create symlink. Path may be relative
   */
  public int symlink(int to_p, int from_p) {
    // if (!mem.bound(to_p) || !mem.bound(from_p))
      // return con.error(efault);
    
    String to = mem.load_string(to_p);
    String from = mem.load_string(from_p);
    opt.info("symlink(%s,%s)", to, from);
    
    if (to == null || from == null)
      return con.error(efault);
    if (to.length() == 0)
      return con.error(enoent);
    
    String toAbs = files.abspath(to);
    String acc = FileSystem.get().access(toAbs, 0);
    if (acc != null) {
      opt.warn("symlink: target %s: %s", toAbs, acc);
      return con.error(eaccess);
    }
    
    // to may be relative
    String err = FileSystem.get().symlink(to, files.abspath(from));
    return con.error(err);
  }
  
  public int sync() {
    opt.info("sync: does nothing...");
    return 0;
  }
  
  /**
   * Delete file
   */
  public int unlink(int path_p) {
    String path = mem.load_string(path_p);
    opt.warn("unlink(%s)", path);
    if (path == null)
      return con.error(efault);
    if (path.length() == 0)
      return con.error(enoent);
    
    String ret = FileSystem.get().unlink(files.abspath(path));
    return con.error(ret);
  }
  
}
