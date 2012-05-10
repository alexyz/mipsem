/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;

import cfern.Driver;
import cfern.sys.str.*;

/**
 * A socket or server socket. Must be call connect/accept before read/write.
 * TODO all methods should be synchronized really
 */
class SocketFileDesc extends FileDesc {
  
  /**
   * Either none or exactly one of sockChan and servSockChan will be not null
   */
  private SocketChannel sockChan;
  private ByteBuffer in, out;
  private Socket sock;
  
  private ServerSocketChannel servSockChan;
  private ServerSocket servSock;
  private LinkedList<SocketChannel> queue;
  
  /** 
   * Create a socket descriptor
   */
  public SocketFileDesc() {
    super();
  }
  
  /** 
   * Create a socket descriptor for the given connection
   */
  public SocketFileDesc(SocketChannel sc) {
    this();
    setSockChan(sc);
  }
  
  /**
   * Set the socket, required before connect/reads/writes
   */
  private void setSockChan(SocketChannel sc) {
    if (servSockChan != null)
      throw new RuntimeException("already has a server socket");
    if (sockChan != null)
      throw new RuntimeException("already has a socket");
    
    sockChan = sc;
    sock = sc.socket();
    out = ByteBuffer.wrap(new byte[4096]);
    in = ByteBuffer.wrap(new byte[4096]);
    // set limit to 0 so it forces a socket read
    in.flip();
  }
  
  /**
   * Open a server socket
   */
  private void openServSockChan() throws IOException {
    if (servSockChan != null)
      throw new RuntimeException("already has a server socket");
    if (sockChan != null)
      throw new RuntimeException("already has a socket");
    
    servSockChan = ServerSocketChannel.open();
    servSock = servSockChan.socket();
    queue = new LinkedList<SocketChannel>();
  }
  
  /**
   * Bind server socket to given socket address
   */
  public String bind(String host, int port) {
    // what if it's already connected as a socket?
    
    SocketAddress sa = new InetSocketAddress(host, port);
    try {
      if (servSockChan == null)
        openServSockChan();
      
      Driver.opt().info("binding to %s...", sa);
      servSock.bind(sa);
      Driver.opt().info("bound");
      return null;
      
    } catch (IOException e) {
      Driver.opt().warn("could not bind to %s", sa);
      e.printStackTrace();
      return eaddressinuse;
    }
  }
  
  /**
   * Listen for connections to this server socket
   */
  public String listen() {
    if (servSockChan == null) {
      Driver.opt().error("accept but no bind");
      return edestaddrreq;
    }
    
    // not sure if we should do anything
    // but calls to available should return number of pending connections
    return null;
  }
  
  /**
   * Get the next connection, optionally blocking if there are none
   */
  public int acceptNext(boolean block) {
    if (queue.size() > 0)
      return queue.size();
    
    try {
      servSockChan.configureBlocking(block);
      if (block)
        Driver.opt().info("accepting...", block);
      SocketChannel sc = servSockChan.accept();
      if (sc != null) {
        Driver.opt().info("accepted %s", sc);
        queue.add(sc);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return queue.size();
  }
  
  /**
   * Get the next connection, blocks if available() == 0
   */
  public SocketFileDesc accept() {
    if (servSockChan == null)
      // should return einvalid
      throw new RuntimeException("not listening");
    
    acceptNext(true);
    SocketChannel sc = queue.removeFirst();
    return new SocketFileDesc(sc);
  }
  
  /**
   * Connect this socket
   */
  public String connect(String host, int port) {
    try {
      if (sockChan == null)
        setSockChan(SocketChannel.open());
      if (sockChan.isConnected())
        return eisconn;
      InetSocketAddress addr = new InetSocketAddress(host, port);
      Driver.opt().info("connecting to %s...", addr);
      sockChan.connect(addr);
      Driver.opt().info("connected");
      return null;
      
    } catch (IOException e) {
      // return appropriate error code
      Driver.opt().warn("could not connect to %s:%s", host, port);
      e.printStackTrace();
      return econnrefused;
    }
  }
  
  /**
   * Yes we are a socket.
   */
  public boolean issock() {
    return true;
  }
  
  public void setSocketOption(SockOpt so) {
    // FIXME keep these
    Driver.opt().warn("setSocketOption: ignoring %s", so);
  }
  
  /**
   * Is socket available for reading?
   */
  public boolean readable() {
    return sockChan != null && sockChan.isConnected() && !sock.isInputShutdown();
  }
  
  /**
   * Is socket available for writing?
   */
  public boolean writeable() {
    return sockChan != null && sockChan.isConnected() && !sock.isOutputShutdown();
  }
  
  /**
   * Sockets are not seekable
   */
  public boolean seekable() {
    return false;
  }
  
  public int available() {
    if (servSockChan != null) {
      // if its a socket channel, return number of pending connections
      return acceptNext(false);
      
    } else if (sockChan != null) {
      // return number of bytes that can be read without blocking
      if (in.hasRemaining()) {
        //Driver.opt().info("UnixSocket.available: buffer has %d bytes", in.remaining());
        return 1;
      }
      try {
        int rd = readImp(false);
        //Driver.opt().info("UnixSocket.available: read %d bytes", rd);
        return rd;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return 0;
  }

  public byte read() throws IOException {
    if (sockChan == null)
      throw new RuntimeException("read but not readable");
    
    if (in.hasRemaining())
      return in.get();
    
    int rd = readImp(true);
    if (rd == -1)
      throw new EOFException();
    return in.get();
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    if (sockChan == null)
      throw new RuntimeException("read but not readable");
    
    if (len <= 0)
      return 0;
    
    // if there is data in the buffer, return that
    if (in.hasRemaining()) {
      int r = Math.min(in.remaining(), len);
      for (int n = 0; n < r; n++)
        buf[off + n] = in.get();
      return r;
    }
    
    // otherwise fill buffer and call this method again
    int rd = readImp(true);
    return (rd <= 0) ? rd : read(buf, off, len);
  }
  
  /**
   * Actually read bytes from socket into buffer.
   * Returns number of bytes read.
   */
  private int readImp(boolean block) throws IOException {
    sockChan.configureBlocking(block);
    in.clear();
    //Driver.opt().info("UnixSocket.readImp: reading, block: %s", block);
    int rd = sockChan.read(in);
    //Driver.opt().info("UnixSocket.readImp: read %d bytes", out.position());
    in.flip();
    return rd;
  }
  
  /**
   * Actually read bytes from socket into buffer.
   * Returns number of bytes read.
   */
  private int writeImp(boolean block) throws IOException {
    sockChan.configureBlocking(block);
    out.flip();
    //Driver.opt().info("UnixSocket.writeImp: writing %d bytes, block: %s", out.remaining(), block);
    int rd = sockChan.write(out);
    out.clear();
    return rd;
  }
  
  /**
   * Write a single byte
   */
  public void write(byte b) throws IOException {
    if (sockChan == null)
      throw new RuntimeException("write but not writeable");
    
    out.put(b);
    writeImp(true);
  }
  
  /**
   * Write an array of bytes
   */
  public void write(byte[] buf, int off, int len) throws IOException {
    if (sockChan == null)
      throw new RuntimeException("write but not writeable");
    
    for (int n = 0; n < len; n++) {
      out.put(buf[off + n]);
      if (!out.hasRemaining())
        writeImp(true);
    }
    writeImp(true);
  }
  
  protected void closeImp() throws IOException {
    if (sockChan != null)
      sockChan.close();
    if (servSockChan != null)
      servSockChan.close();
  }
  
  public SelectableChannel inChannel() {
    // not sure if these should be done
    return sockChan;
  }
  
  public SelectableChannel outChannel() {
    // not sure if sockChan is correct
    // as it's output may be buffered...
    return sockChan != null ? sockChan : servSockChan;
  }
  
  public Stat fstat() {
    throw new RuntimeException("socket stat unimplemented");
  }
  
  public StatFS fstatfs() {
    throw new RuntimeException("socket fstatfs unimplemented");
  }
  
  public String toString() {
    return String.format("%s[%s]", super.toString(), (sock != null) ? "Socket" : "ServerSocket");
  }
  
} // end of class UnixSocket
