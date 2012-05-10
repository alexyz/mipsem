/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.io;
import java.io.IOException;
import java.nio.channels.*;
import java.util.*;
import cfern.Driver;
import cfern.fs.FileDesc;

public class MySelector {

  private final Selector sel;
  private final short[] avFd;
  private final FileDesc[] av;
  private short[] avRet = null;
  
  public static MySelector newInstance(FileDesc[] in, short[] inFd, FileDesc[] out, short[] outFd) {
    short[] avFd = null, rdFd = null;
    FileDesc[] av = null, rd = null;
    
    if (in != null) {
      // count how many read fds have channels
      int c = 0;
      for (int n = 0; n < in.length; n++)
        if (in[n].inChannel() != null)
          c++;
      // make arrays for channel read fds and non channel read fds
      if (c > 0) {
        rd = new FileDesc[c];
        rdFd = new short[c];
      }
      if (c < in.length) {
        av = new FileDesc[in.length - c];
        avFd = new short[in.length - c];
      }
      // copy read fds to new arrays
      for (int n = 0, i = 0, j = 0; n < in.length; n++) {
        if (in[n].inChannel() != null) {
          rd[i] = in[n];
          rdFd[i++] = inFd[n];
        } else {
          av[j] = in[n];
          avFd[j++] = inFd[n];
        }
      }
    }
    
    if (out != null) {
      // check all write fds have channels
      for (int n = 0; n < out.length; n++) {
        if (out[n].outChannel() == null)
          throw new RuntimeException("cannot select writes on " + out[n]);
      }
    }
    
    Driver.opt().info("MySelector: av=%s rd=%s wr=%s", 
        Arrays.toString(avFd), Arrays.toString(rdFd), Arrays.toString(outFd));
    if (rd == null && av == null && out == null)
      return null;
    else
      return new MySelector(av, avFd, rd, rdFd, out, outFd);
  }
  
  private MySelector(FileDesc[] av, short[] avFd, FileDesc[] rd, short[] rdFd, FileDesc[] wr, short[] wrFd) {
      Selector sel = null;
      if (rd != null || wr != null) {
        try {
          sel = Selector.open();
          if (rd != null) {
            for (int n = 0; n < rd.length; n++) {
              FileDesc f = rd[n];
              SelectableChannel sc = f.inChannel();
              sc.configureBlocking(false);
              // TODO should probably be OP_ACCEPT if sc is ServerSocketChannel
              sc.register(sel, SelectionKey.OP_READ, new FD(FD.R, rdFd[n]));
            }
          }
          if (wr != null) {
            for (int n = 0; n < wr.length; n++) {
              FileDesc f = wr[n];
              SelectableChannel sc = f.outChannel();
              sc.configureBlocking(false);
              sc.register(sel, SelectionKey.OP_WRITE, new FD(FD.W, wrFd[n]));
            }
          }
        } catch (IOException e) {
          throw new RuntimeException("could not create write selector", e);
        }
      }
      
      // may be null
      this.avFd = avFd;
      this.av = av;
      this.sel = sel;
  }
  
  public short[][] select() {
    selectImpl();
    return getSelected();
  }
  
  private int selectImpl() {
    int ret = 0;
    while (true) {
      if (av != null) {
        for (int n = 0; n < av.length; n++) {
          FileDesc f = av[n];
          if (f.available() != 0) {
            Driver.opt().info("select: read possible on %s", f, f.available());
            avRet = Driver.append(avRet, avFd[n]);
            ret++;
          }
        }
      }
      if (sel != null) {
        try {
          if (ret == 0) {
            if (av == null) {
              // select forever
              ret += sel.select();
            } else {
              // select and sleep for next av poll
              ret += sel.select(100);
            }
          } else {
            // results from av waiting, don't delay
            ret += sel.selectNow();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // FIXME select needs to return EINTR
          e.printStackTrace();
        }
      }
      if (ret > 0) {
        Driver.opt().info("select: %d fds ready", ret);
        return ret;
      }
    }
  }
  
  private short[][] getSelected() {
    short[][] ret = new short[][] { avRet, null };
    
    if (sel != null) {
      Iterator<SelectionKey> i = sel.selectedKeys().iterator();
      for (int n = 0; i.hasNext(); n++) {
        FD f = ((FD)i.next().attachment());
        ret[f.type] = Driver.append(ret[f.type], f.fd);
      }
      try {
        sel.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    // sort if many.. which I doubt
    for (int n = 0; n < 2; n++) {
      short[] s = ret[n];
      if (s != null && s.length > 1)
        Arrays.sort(s);
    }
    
    Driver.opt().info("select: returning rd=%s wr=%s",
        Arrays.toString(ret[0]), Arrays.toString(ret[1]));
    return ret;
  }
  
}

class FD {
  public final static short R = 0, W = 1;
  public final short fd;
  public final short type;
  FD(short type, short fd) {
    this.type = type;
    this.fd = fd;
  }
}
  
