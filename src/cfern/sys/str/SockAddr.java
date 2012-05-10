/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.sys.str;
import cfern.mem.Memory;
import cfern.sys.Constants;

/**
 * A class representing a socket address (only inet supported)
 */
public class SockAddr extends Struct {

  public int family;
  public int port;
  public int addr;
  public String host;

  /**
   * Load structure from memory
   */
  public SockAddr load(Memory mem, int sa_p) {
    family = Struct.load_field(mem, "sockaddr", "sa_family", sa_p);
    // interpretation of sa_p depends on family
    if (Constants.get().is(family, "PF_INET")) {
      port = load_field(mem, "sockaddr_in", "sin_port", sa_p);
      // this is actually a struct in a struct
      addr = load_field(mem, "sockaddr_in", "sin_addr", sa_p);
      host = hostForAddress(addr);
    } else {
      throw new RuntimeException("unknown family " + family);
    }
    return this;
  }

  /**
   * Convert int address to string address
   */
  public static String hostForAddress(int addr) {
    return String.format("%d.%d.%d.%d", addr >>> 24, (addr >>> 16) & 0xff, (addr >>> 8) & 0xff, addr & 0xff);
  }

  public String toString() {
    return String.format("SockAddr[%s:%d]", host, port);
  }

}
