/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import cfern.sys.str.*;

/**
 * A unix inter process pipe file descriptor.
 * Inherits from UnixStream but has slightly different fstat results
 */
class PipeFileDesc extends StreamFileDesc {
  
  /** 
   * Create a pipe
   */
  public PipeFileDesc(OutputStream outs, InputStream ins) {
    super(ins, outs);
  }
  
  /**
   * Pipes are not char devices like most streams
   */
  public boolean ischr() {
    return false;
  }
  
  /**
   * Pipes are fifos
   */
  public boolean isfifo() {
    return true;
  }
  
  public Stat fstat() {
    /*
    fstat of piperd (8):
      dev=6   ino=9081   mode=4480  nln=1 rdev=0     size=0    bs=4096 nb=0
      sock=0 ln=0 reg=0 blk=0 dir=0 chr=0 fifo=1
    fstat of pipewr (9):
      dev=6   ino=9081   mode=4480  nln=1 rdev=0     size=0    bs=4096 nb=0
      sock=0 ln=0 reg=0 blk=0 dir=0 chr=0 fifo=1
    */
    Stat st = new Stat();
    int time = (int) (System.currentTimeMillis() / 1000L);
    st.atime = time;
    st.blksize = 4096;
    st.blocks = 0;
    st.ctime = time;
    // not sure about this or inode
    st.dev = 6;
    st.gid = 0;
    st.inode = 9999;
    st.mode = Stat.modefor(true, true, true, Stat.fifo);
    st.mtime = time;
    st.nlink = 1;
    st.rdev = 0;
    st.size = 0;
    st.uid = 0;
    return st;
  }
  
  public StatFS fstatfs() {
    // this does work for pipes on linux, most values are zero
    throw new RuntimeException("pipe fstatfs unimplemented");
  }
  
} // end of class UnixPipe
