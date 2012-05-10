/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import cfern.fs.*;
import cfern.sys.str.SockAddr;

/**
 * Methods for <sys/stat.h>
 */
public class SocketInterface extends Interface {
  
  public SocketInterface(SystemInterface sys) {
    super(sys);
  }
  
  public SocketInterface(SystemInterface sys, SocketInterface wait) {
    super(sys);
  }
  
  /**
   * Accept connection
   * int accept(int sockfd, struct sockaddr *addr, socklen_t *addrlen);
   */
  public int accept (int fd, int sa_p, int len) {
    FileDesc ss = files.getfd(fd);
    opt.warn("accept (%d: %s, %s, %d)", fd, ss, mem.getname(sa_p), len);
    if (ss == null)
      return con.error(ebadf);

    FileDesc s = ss.accept();
    if (s == null)
      return con.error(enotsock);

    if (sa_p != 0)
      throw new RuntimeException("fill in sa...");
    
    int sfd = files.newfd(s);
    return sfd;
  }
  
  /**
   * Bind a socket
   */
  public int bind(int fd, int sa_p, int len) {
    FileDesc file = files.getfd(fd);
    opt.warn("bind (%d: %s, %s, %d)", fd, file, mem.getname(sa_p), len);
    if (file == null)
      return con.error(ebadf);
    if (!mem.bound(sa_p))
      return con.error(efault);
    
    SockAddr sa = new SockAddr().load(mem, sa_p);
    if (con.is(sa.family, "PF_INET")) {
      opt.warn("bind: loaded %s", sa);
      String err = file.bind(sa.host, sa.port);
      return con.error(err);

    } else {
      throw new RuntimeException("can only bind inet sockets");
    }
  }
  
  /**
   * Connect a socket
   * int connect(int sockfd, const struct sockaddr *serv_addr, socklen_t addrlen);
   */
  public int connect (int fd, int sa_p, int len) {
    FileDesc file = files.getfd(fd);
    opt.warn("connect (%d: %s, %s, %d)", fd, file, mem.getname(sa_p), len);
    if (file == null)
      return con.error(ebadf);
    if (!mem.bound(sa_p))
      return con.error(efault);

    SockAddr sa = new SockAddr().load(mem, sa_p);
    
    if (con.is(sa.family, "PF_INET")) {
      String err = file.connect(sa.host, sa.port);
      return con.error(err);
      
    } else {
      throw new RuntimeException("can only connect inet sockets");
    }
  }
  
  /**
   * Listen for connections to server socket
   */
  public int listen (int fd, int backlog) {
    FileDesc sock = files.getfd(fd);
    opt.warn("listen (%d: %s, %d) does nothing", fd, sock, backlog);
    if (sock == null)
      return con.error(ebadf);

    // does nothing...
    return 0;
  }
  
  /**
   * Set socket option.
   */
  public int setsockopt(int fd, int lev, int opt_, int optval_p, int len) {
    FileDesc f = files.getfd(fd);
    opt.info("setsockopt(fd %d: %s, lvl %s, opt %s, optval %s, len %d)", 
        fd, f, con.namerx("SOL_SOCKET|IPPROTO_TCP", lev), con.names("SO_", opt_), mem.getname(optval_p), len);
    
    if (f == null)
      return con.error(ebadf);
    if (!f.issock())
      return con.error(enotsock);
    if (!mem.bound(optval_p))
      return con.error(efault);
    int optval = mem.load_word(optval_p);
    
    if (con.is(opt_, "SO_KEEPALIVE")) {
      // TODO parameterise options
      if (optval != 0)
        f.setSocketOption(FileDesc.SockOpt.keepAlive);
    } else if (con.is(opt_, "SO_REUSEADDR")) {
      if (optval != 0)
        f.setSocketOption(FileDesc.SockOpt.reuseAddr);
    } else {
      throw new RuntimeException("unknown setsockopt");
    }
    
    return 0;
  }
  
  /**
   * Create a socket.
   * int socket(int domain, int type, int protocol).
   */
  public int socket(int domain, int type, int prot) {
    opt.warn("socket(%s,%s,%d)", con.name("PF_", domain), con.name("SOCK_", type), prot);

    // for domains see linux/include/linux/socket.h
    if (con.is(domain, "PF_LOCAL")) {
      return con.error(eaccess);
      
    } else if (con.is(domain, "PF_INET")) {
      FileDesc sock = FileSystemUtil.socket();
      return files.newfd(sock);

    } else if (con.is(domain, "PF_INET6")) {
      throw new RuntimeException("ipv6, seriously...");
    }
    
    throw new RuntimeException("unknown domain " + domain);
  }
  
  
}























