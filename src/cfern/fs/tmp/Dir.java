/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;
import java.util.*;

import cfern.Driver;
import cfern.fs.FileSystemUtil;
import cfern.sys.str.*;

/**
 * A virtual directory backed by a TreeMap.
 * FIXME must be synchronized
 */
public class Dir extends File {

  /**
   * The directory content
   */
  private final Map<String, File> content = new TreeMap<String, File>();

  /**
   * Create virtual directory
   * Must call setfs() if not using Dir.put!
   */
  public Dir() {
    //
  }
  
  /**
   * Returns this
   */
  public Dir dir() {
    return this;
  }

  /**
   * Get directory for given directory path, optionally creating. May return
   * null in either case.
   */
  public Dir getdir(String path, boolean make) {
    String[] patha = FileSystemUtil.split(path);
    return getdir(patha, make, patha.length);
  }

  /**
   * Get the directory for the given file path (i.e. ignoring the last element),
   * optionally creating if not found. May return null in either case.
   */
  public Dir getdirfor(String[] path, boolean make) {
    return getdir(path, make, path.length - 1);
  }
  
  /**
   * Get directory for subset of given file path, optionally creating. May return
   * null in either case.
   */
  private Dir getdir(String[] path, boolean make, int limit) {
    Dir ret = this;
    // iterate over each path element
    for (int n = 0; n < limit; n++) {
      // empty strings refer to current dir
      if (path[n].length() == 0)
        continue;
      
      File f = ret.get(path[n]);
      if (f == null) {
        if (!make)
          return null; // return Option.dirNotFound
        // not found, create dir
        ret.put(path[n], f = new Dir());
      } else {
        Link l = f.link();
        if (l != null) {
          String rem = Arrays.toString(Arrays.copyOfRange(path, n + 1, path.length));
          Driver.opt().warn("should follow link to %s %s", l.target(), rem);
          Thread.dumpStack();
          // can't follow link, may be on other fs
          return null; // return Option.forLink("...")
        } else if (f.dir() == null) {
          // not a dir or link
          return null; // return Option.notDir
        }
      }
      ret = f.dir();
    }
    return ret;
  }
  
  /**
   * Get file in this directory only.
   * Returns null if file doesn't exist.
   */
  File get(String name) {
    return get(name, false);
  }
  
  /**
   * Remove (and returns) given file.
   * Returns null if file doesn't exist.
   */
  public File remove(String name) {
    return get(name, true);
  }

  /**
   * Get or remove file in this directory only.
   * Returns null if file doesn't exist.
   */
  private synchronized File get(String name, boolean remove) {
    // every directory is it's own root, don't think this is relied on though
    if (name.equals("/"))
      return this;
    if (name.indexOf("/") >= 0)
      throw new RuntimeException("must be filename only");
    return remove ? content.remove(name) : content.get(name);
  }
  
  /**
   * Get file/directory/stream for given path. Returns null if file not found.
   */
  File getfile(String path) {
    return getfile(FileSystemUtil.split(path));
  }
  
  /**
   * Get file/directory/stream for given path. Returns null if file not found.
   */
  private File getfile(String[] path) {
    //Machine.opt().info("Dir.getfile(%s) len=%d", Arrays.toString(path), path.length);
    // path of "/" split produces empty array
    if (path.length == 0)
      return this; // return Option.dirFor(this)
    Dir d = getdirfor(path, false);
    // TODO distinguish between enoent and enotdir
    if (d == null) {
      //Machine.opt().info("Dir.getfile(%s) - could not get directory", Arrays.toString(path));
      return null;
    }
    File f = d.get(path[path.length - 1]);
    return f; // return Option.forFile(f)
  }

  /**
   * Store the given File in this directory.
   * Returns false if file name already exists (nothing is changed).
   */
  public synchronized boolean put(String name, File f) {
    if (name == null || name.length() == 0 || name.indexOf("/") >= 0)
      throw new RuntimeException("not a filename: " + name);
    if (content.containsKey(name))
      return false;
    content.put(name, f);
    f.setfs(getfs());
    return true;
  }
  
  /**
   * Return a list of files in this directory
   */
  /*
  synchronized String[] list() {
    String[] files = new String[content.size()];
    Iterator<String> iter = content.keySet().iterator();
    for (int n = 0; iter.hasNext(); n++)
      files[n] = iter.next();
    return files;
  }
  */
  
  /**
   * Return a list of files in this directory
   */
  synchronized DirEnt[] list() {
    DirEnt[] ret = new DirEnt[content.size()];
    Iterator<Map.Entry<String,File>> iter = content.entrySet().iterator();
    for (int n = 0; iter.hasNext(); n++) {
      Map.Entry<String,File> e = iter.next();
      String name = e.getKey();
      File f = e.getValue();
      
      DirEnt de = new DirEnt();
      de.inode = f.inode;
      de.name = name;
      de.offset = n;
      de.type = f.dir() != null ? DirEnt.DIR : DirEnt.FILE;
      ret[n] = de;
    }
    return ret;
  }
  
  Stat stat() {
    // TODO needs more fields
    return stat(Stat.dir);
  }

} // end of class Dir
