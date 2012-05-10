package cfern.sys;

/**
 * General error codes, no methods.
 * See http://lxr.linux.no/source/include/asm-generic/errno-base.h
 */
public interface Errors {
  
  public final static String eperm = "EPERM";
  /** File not found */
  public final static String enoent = "ENOENT";
  public final static String esrch = "ESRCH";
  public final static String eintr = "EINTR";
  /** Bad access mode for device */
  public final static String eio = "EIO";
  public final static String enxio = "ENXIO";
  public final static String etoobig = "E2BIG";
  public final static String enoexec = "ENOEXEC";
  /** File not open */
  public final static String ebadf = "EBADF";
  public final static String echild = "ECHILD";
  public final static String eagain = "EAGAIN";
  public final static String enomem = "ENOMEM";
  /** Access denied */
  public final static String eaccess = "EACCES";
  /** Bad address argument to syscall */
  public final static String efault = "EFAULT";
  public final static String enotblock = "ENOTBLK";
  public final static String ebusy = "EBUSY";
  /** Tried to create a file that already exists */
  public final static String eexists = "EEXIST";
  /** Crossed file system devices */
  public final static String exdev = "EXDEV";
  public final static String enodev = "ENODEV";
  public final static String enotdir = "ENOTDIR";
  /** Tried to open a directory for writing */
  public final static String eisdir = "EISDIR";
  /** Illegal argument */
  public final static String einvalid = "EINVAL";
  public final static String enfile = "ENFILE";
  public final static String emfile = "EMFILE";
  public final static String enottty = "ENOTTY";
  public final static String etxtbusy = "ETXTBSY";
  public final static String efbig = "EFBIG";
  public final static String enospace = "ENOSPC";
  public final static String eispipe = "ESPIPE";
  public final static String erofs = "EROFS";
  public final static String emlink = "EMLINK";
  public final static String epipe = "EPIPE";
  public final static String edom = "EDOM";
  public final static String erange = "ERANGE";
  /** File is not a socket */
  public static final String enotsock = "ENOTSOCK";
  /** Can't reopen a socket */
  public static final String eisconn = "EISCONN";
  /** Could not connect socket */
  public static final String econnrefused = "ECONNREFUSED";
  /** Socket address in use */
  public static final String eaddressinuse = "EADDRINUSE";
  /** Socket not bound */
  public static final String edestaddrreq = "EDESTADDRREQ";
  /** System call not supported */
  public static final String enosys = "ENOSYS";
}
