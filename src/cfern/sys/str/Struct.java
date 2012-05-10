
package cfern.sys.str;
import cfern.Driver;
import cfern.mem.Memory;
import cfern.sys.Constants;

/**
 * Abstract C struct. Has only static methods as these objects are never used
 * generically. By convention subclasses should have init(...), load(mem, addr)
 * and store(mem, addr) methods.
 * TODO make static fields package visible
 */
public abstract class Struct {
  
  /**
   * Store an byte/half/int/dword value into a struct
   */
  public static void store_field(Memory mem, String struct, String field, int struct_p, int val) {
    store_field(mem, struct, field, struct_p, val, false);
  }

  /**
   * Store an byte/half/int/dword value into a struct, optionally ignoring field
   * size
   */
  public static void store_field(Memory mem, String struct, String field, int struct_p, int val, boolean asword) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    int field_p = struct_p + vals[0];
    switch (vals[1]) {
      case 1:
        mem.store_byte(field_p, (byte) val);
        break;
      case 2:
        mem.store_half(field_p, (short) val);
        break;
      case 4:
        mem.store_word(field_p, val);
        break;
      case 8:
        mem.store_dword(field_p, val);
        break;
      default:
        if (asword)
          mem.store_word(field_p, val);
        else
          throw new RuntimeException("invalid field size for " + field + ": " + vals[1]);
    }
  }
  
  /**
   * Store long into a struct
   */
  public static void store_field(Memory mem, String struct, String field, int struct_p, long val) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    int field_p = struct_p + vals[0];
    if (vals[1] == 8)
      mem.store_dword(field_p, val);
    else
      throw new RuntimeException("invalid field size for " + field + ": " + vals[1]);
  }
  
  /**
   * Store an byte array into a struct.
   */
  public static void store_field(Memory mem, String struct, String field, int struct_p, byte[] val) {
    store_field(mem, struct, field, struct_p, val, val.length);
  }
  
  /**
   * Store exactly len bytes of a byte array into a struct.
   */
  public static void store_field(Memory mem, String struct, String field, int struct_p, byte[] val, int len) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    if (val.length > vals[1])
      throw new RuntimeException(String.format("field %s.%s len=%d but val.length=%d", 
          struct, field, val.length, vals[1]));
    int field_p = struct_p + vals[0];
    mem.store_bytes(field_p, val, 0, len);
  }
  
  /**
   * Store a string value into a struct.
   * Returns number of bytes written plus offset.
   */
  public static int store_field(Memory mem, String struct, String field, int struct_p, String val) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    if (val.length() > vals[1]) {
      Driver.opt().error("store_field: string \"%s\" too long for %s", val, field);
      val = val.substring(0, vals[1] - 1);
    }
    int field_p = struct_p + vals[0];
    return vals[0] + mem.store_string(field_p, val);
  }
  
  /**
   * Load an byte/half/word value from a struct
   */
  public static int load_field(Memory mem, String struct, String field, int struct_p) {
    return load_field(mem, struct, field, struct_p, false);
  }

  /**
   * Load an byte/half/word value from a struct, optionally ignoring size
   */
  public static int load_field(Memory mem, String struct, String field, int struct_p, boolean asword) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    int field_p = struct_p + vals[0];
    switch (vals[1]) {
      case 1:
        return mem.load_byte(field_p);
      case 2:
        return mem.load_half(field_p);
      case 4:
        return mem.load_word(field_p);
      default:
        if (asword)
          return mem.load_word(field_p);
    }
    throw new RuntimeException("invalid field size for " + field + ": " + vals[1]);
  }
  
  /**
   * Load a byte array from a struct
   */
  public static void load_field(Memory mem, String struct, String field, int struct_p, byte[] val) {
    Constants con = Constants.get();
    int[] vals = con.geta(struct + "." + field);
    if (val.length > vals[1])
      throw new RuntimeException(String.format("field %s.%s len=%d but val.length=%d", 
          struct, field, val.length, vals[1]));
    int field_p = struct_p + vals[0];
    mem.load_bytes(field_p, val, 0, val.length);
  }

}
