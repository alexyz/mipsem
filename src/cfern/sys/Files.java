/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.util.regex.Pattern;

import cfern.Driver;
import cfern.fs.*;

/**
 * Keeps track of open files for a process.
 * Use getfd to make sure a file descriptor is valid before using fd methods.
 */
class Files {
  
  /**
   * Unix/zip directory seperator
   */
  protected static final Pattern sep = Pattern.compile("/");
  
  /**
   * Open files, including stdin/out/err, pipes, and real files.
   */
  private final FileDesc[] files;
  
  /**
   * File desciptor flags (of which there is only one, close on exec).
   * Naturally this is set to false after exec.
   */
  private final boolean[] filedes;
  
  /**
   * Current working directory, unix style, must end in /
   */
  private String cwd;
  
  /**
   * File creation mask, default is 0777
   */
  private int umask;
  
  /**
   * New files object.
   */
  Files() {
    cwd = "/";
    files = new FileDesc[32];
    filedes = new boolean[32];
    umask = 0777;
  }
  
  /**
   * Copy files object on fork.
   */
  Files(Files other) {
    files = other.files.clone();
    filedes = other.filedes.clone();
    cwd = other.cwd;
    umask = other.umask;
    
    // increase ref count of open files
    for (int n = 0; n < files.length; n++)
      if (files[n] != null) 
        files[n].dup();
  }
  
  /**
   * Set the new current working directory
   */
  void setcwd (String cwd) {
    this.cwd = cwd;
  }
  
  /**
   * Get the current working directory
   */
  String getcwd() {
    return cwd;
  }
  
  /**
   * Convert a relative unix path to an absolute one with respect
   * to current working directory.
   */
  String abspath(String path) {
    // it's up to the syscall to check for blank
    if (path.length() == 0)
      throw new RuntimeException("path is blank");
    return path.startsWith("/") ? path : cwd.concat(path);
  }
  
  /**
   * Add this file descriptor to the list of open files.
   */
  int newfd(FileDesc file) {
    if (file == null)
      throw new RuntimeException("file is null");
    for (int n = 0; n < files.length; n++) {
      if (files[n] == null) {
        Driver.opt().info("new_fd %d", n);
        files[n] = file;
        filedes[n] = false;
        return n;
      }
    }
    throw new RuntimeException("out of file handles");
  }
  
  /**
   * Close the file fd, if any, and replace with the given file.
   */
  void replacefd(int fd, FileDesc file) {
    FileDesc old = getfd(fd, false);
    if (old != null)
      old.close();
    
    files[fd] = file.dup();
    filedes[fd] = false;
  }
  
  /**
   * Get the file descriptors for this set of files.
   * Returns null if fds is null.
   */
  FileDesc[] getfds(short[] fds) {
    if (fds == null)
      return null;
    FileDesc[] ret = new FileDesc[fds.length];
    for (int n = 0; n < fds.length; n++)
      ret[n] = getfd(fds[n]);
    return ret;
  }
  
  /**
   * Get open file or null with error message if not open
   */
  FileDesc getfd(int fd) {
    return getfd(fd, true);
  }
  
  /**
   * Get open file or null with optional error message if not open
   */
  FileDesc getfd(int fd, boolean err) {
    if ((fd < 0) || (fd > files.length)) {
      if (err) {
        Driver.opt().error("getfd: %d just weird", fd);
        Thread.dumpStack();
      }
      return null;
    }
    
    FileDesc ret = files[fd];
    if (ret == null && err)
      Driver.opt().error("getfd: %d not open", fd);
    return ret;
  }
  
  /**
   * Return the number of files currently open
   */
  int open() {
    int ret = 0;
    for (int n = 3; n < files.length; n++)
      if (files[n] != null) // should check if refcount = 1
        ret++;
    return ret;
  }
  
  /**
   * Flush and close all remaining files.
   * If exec then only closes files marked close on exec.
   */
  void closeall (boolean exec) {
    if (exec) {
      for (int n = 0; n < files.length; n++) {
        if (files[n] != null && filedes[n]) {
          Driver.opt().info("execve: closing %s on exec", files[n]);
          close(n);
        }
      }
      return;
    }
    
    for (int n = 0; n < files.length; n++)
      if (files[n] != null)
        close(n);
  }
  
  /**
   * Close and remove given file. 
   */
  void close (int fd) {
    files[fd].close();
    files[fd] = null;
    filedes[fd] = false;
  }
  
  /**
   * Tell this file to close on exec.
   * This is the only file descriptor option.
   */
  void setCloseOnExec (int fd, boolean cl) {
    filedes[fd] = cl;
  }
  
  /**
   * Get the close on exec flag
   */
  boolean getCloseOnExec (int fd) {
    return filedes[fd];
  }
  
  /**
   * Get the file creation mask, max value is 0777
   */
  int getumask() {
    return umask;
  }
  
  /**
   * Set the file creation mask, bits other than 0777 are ignored
   */
  void setumask(int newm) {
    umask = newm & 0777;
  }
  
}
