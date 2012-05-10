/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs;
import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

import cfern.Driver;
import cfern.io.*;

/**
 * Static methods for file systems.
 */
public final class FileSystemUtil {

  /**
   * Unix/zip directory separator (see split())
   */
  private static final Pattern sep = Pattern.compile("/");
  
  private static final Pattern windows = Pattern.compile("\\w:\\\\");
  
  private FileSystemUtil() {
    // private to prevent javadoc
  }

  /**
   * Create a new SocketFileDesc
   */
  public static FileDesc socket() {
    return new SocketFileDesc();
  }

  /**
   * Create an array out of the given path
   */
  public static String[] split(String path) {
    // path should be at least /
    if (path.length() == 0)
      throw new RuntimeException("cannot split empty string");
    String[] ret = FileSystemUtil.sep.split(path, 0);
    Driver.opt().fslog("split: %s -> %s", path, Arrays.toString(ret));
    if (ret.length == 1 && ret[0].length() == 0)
      throw new RuntimeException("bad path " + path);
    return ret;
  }

  /**
   * Create a pipe. 
   * Returns read end in [0] and write end in [1].
   */
  public static FileDesc[] pipe() {
    FileDesc[] uf = null;
    try {
      PipedOutputStream wr = new PipedOutputStream();
      PipedInputStream rd = new PipedInputStream(wr);
      uf = new FileDesc[] { new PipeFileDesc(OutputStreamUtil.sink, rd), new PipeFileDesc(wr, InputStreamUtil.eof) };
    } catch (IOException e) {
      // will never happen
      throw new RuntimeException("couldn't create pipe", e);
    }
    return uf;
  }

  /**
   * Resolve ., .. and multiple /.
   * Call this when passing a path to any file system except VirtualFileSystem
   */
  public static String reduce(String path) {
    if (path.length() == 0 || !path.startsWith("/"))
      throw new RuntimeException("not an absolute path: " + path);
    StringBuilder sb = new StringBuilder(path.length());
    
    int d = 0, nd = 0;
    while (nd != -1) {
      nd = path.indexOf("/", d + 1);
      String s = nd == -1 ? path.substring(d) : path.substring(d, nd);
      //System.out.println("got " + s);
      
      if (s.equals("/") || s.equals("/.")) {
        // allow one / but not more than one
        if (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')
          sb.append("/");
        
      } else if (s.equals("/..")) {
        int ld = sb.lastIndexOf("/", sb.length() - 2);
        if (ld >= 0)
          sb.setLength(ld + 1);
        
      } else {
        // s starts with /, so trim if there is already one at the end
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/')
          sb.setLength(sb.length() - 1);
        sb.append(s);
      }
      
      d = nd;
    }
    
    String ret = sb.length() == 0 ? "/" : sb.toString();
    Driver.opt().fslog("reduce: %s -> %s", path, ret);
    return ret;
  }
  
  /**
   * Returns true if path is an absolute Windows path (e.g. c:\file.txt)
   */
  public static boolean isWindowsPath(String path) {
    return windows.matcher(path).lookingAt();
  }

}
