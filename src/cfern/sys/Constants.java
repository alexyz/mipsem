/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import cfern.Driver;

/**
 * Loads system interface constants from file generated from constants.c.
 * Multiple values can be associated with a key, e.g. for struct field offset + size.
 * This class is thread safe.
 * File examples:
 * O_RDONLY=0
 * sizeof_iovec=8
 * stat.st_rdev=32,8
 */
public final class Constants {
  
  /** console file descriptors */
  public static final int
    STDIN = 0,
    STDOUT = 1,
    STDERR = 2;
  
  /**
   * Loaded constants. There can only be one.. as constants are for the kernel
   * (linux) not the C library which may vary.
   */
  private static Constants instance;
  
  /**
   * The loaded constants.
   * Array size is always at least 1
   * Doesn't need synchronisation - is read only.
   */
  private final TreeMap<String,int[]> cons = new TreeMap<String, int[]>();
  
  /**
   * Loaded keys and values for iterating
   */
  private final Set<Map.Entry<String, int[]>> entry;
  
  /**
   * Load linux system interface constants for the given library.
   * This should be called only once before first machine starts.
   */
  public static void load(String lib) {
    if (instance != null)
      throw new RuntimeException("cannot reload constants");
    instance = new Constants(lib);
  }
  
  /**
   * Get instance of constants. Never returns null.
   */
  public static Constants get() {
    if (instance == null)
      throw new RuntimeException("constants not loaded");
    return instance;
  }
  
  /**
   * Load constants from given file
   */
  private Constants(String lib) {
    this.entry = cons.entrySet();
    try {
      // load from within package
      InputStream is = Driver.class.getResourceAsStream(lib);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      Pattern pat = Pattern.compile("([\\w\\.]+)=((-?\\d+)(,-?\\d+)*)");
      String line;
      while ((line = br.readLine()) != null) {
        Matcher mat = pat.matcher(line);
        if (mat.matches()) {
          String key = mat.group(1);
          String val = mat.group(2);
          int[] valArr;
          if (val.indexOf(",") == -1) {
            valArr = new int[] { Integer.parseInt(val) };
          } else {
            String[] vals = val.split(",");
            valArr = new int[vals.length];
            for (int n = 0; n < vals.length; n++)
              valArr[n] = Integer.parseInt(vals[n]);
          }
          if (cons.containsKey(key))
            Driver.opt().error("redefinition of " + key);
          cons.put(key, valArr);
        } else if (line.length() > 0 && !line.startsWith("#")) {
          Driver.opt().error("invalid constant \"" + line + "\"");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load library constants from " + lib, e);
    }
  }
  
  /**
   * Returns zero if error is null, otherwise -get(error).
   */
  public int error(String error) {
    if (error != null) {
      if (!error.startsWith("E"))
        throw new RuntimeException("not an error: " + error);
      return -get(error);
    }
    return 0;
  }
  
  /**
   * Get value of constant, throws RuntimeException if not defined.
   * Can pass bitwise or expressions, e.g. "S_IRUSR|S_IWUSR"
   */
  public int get(String key) {
    if (key.indexOf("|") > 0) {
      int val = 0;
      String[] keys = key.split("\\|");
      for (int n = 0; n < keys.length; n++)
        val = val | get(keys[n]);
      return val;
    }
    
    int[] valArr = cons.get(key);
    if (valArr == null) {
      if (Driver.opt().undef)
        // eww
        return 0;
      else
        throw new RuntimeException("system constant " + key + " undefined");
    }
    return valArr[0];
  }
  
  /**
   * Get all values of constant, throws RuntimeException if not defined.
   * Returns array of at least length 2.
   * Do NOT modify array!
   */
  public int[] geta(String key) {
    int[] val = cons.get(key);
    if (val == null || val.length == 1)
      throw new RuntimeException("multiple constant key " + key + " undefined");
    return val;
  }
  
  /**
   * Get the name of a constant, returns number if not defined.
   * FIXME use binary search instead of linear search, same for names
   */
  public String name(String prefix, int val) {
    Iterator<Entry<String,int[]>> iter = entry.iterator();
    
    while (iter.hasNext()) {
      Entry<String,int[]> ent = iter.next();
      String key = ent.getKey();
      if (key.startsWith(prefix)) {
        int con = ent.getValue()[0];
        if (con == val)
          return key;
      }
    }
    //System.err.println("* Could not get name of constant " + prefixlist + " = " + val);
    return Integer.toString(val);
  }
  
  /**
   * Get the name of a constant starting with given regex. Never returns null.
   */
  public String namerx(String regex, int val) {
    Iterator<Entry<String, int[]>> iter = entry.iterator();
    Pattern pat = Pattern.compile(regex);
    
    while (iter.hasNext()) {
      Entry<String,int[]> ent = iter.next();
      String key = ent.getKey();
      Matcher mat = pat.matcher(key);
      if (mat.lookingAt()) {
        int con = ent.getValue()[0];
        if (con == val)
          return key;
      }
    }
    Driver.opt().debug("namerx: could not match %s to %d", regex, val);
    return Integer.toString(val);
  }
  
  /**
   * Get the bitwise or (|) mask of the given constant.
   * Never returns null.
   */
  public String names(String prefix, int valp) {
    int val = valp;
    Iterator<Entry<String,int[]>> iter = entry.iterator();
    StringBuilder sb = new StringBuilder();
    
    while (iter.hasNext()) {
      Entry<String,int[]> ent = iter.next();
      String key = ent.getKey();
      
      if (key.startsWith(prefix)) {
        int con = ent.getValue()[0];
        if (val == 0 && con == 0) {
          // zero constants can only match once
          sb.append(key);
          break;
        }
        if ((val & con) == con) {
          // append name
          if (sb.length() > 0)
            sb.append("|");
          sb.append(key);
          // remove constant from val
          val = val & ~con;
          if (val == 0)
            break;
        }
      }
    }
    if (sb.length() == 0) {
      //System.err.println("* Could not get any names of constants " + prefix + " = " + valp);
      return Integer.toString(val);
    }
    // print any "leftover" value
    if (val != 0)
      sb.append("|").append(val);
    return sb.toString();
  }
  
  /**
   * Return true if constant equals val
   */
  public boolean is(int val, String key) {
    return get(key) == val;
  }
  
  /**
   * Return true if key is in the bitmask of val
   */
  public boolean has(int val, String key) {
    int con = get(key);
    if (val == 0)
      return val == con;
    else
      return (val & con) == con;
  }
  
} // end of SystemConstants
