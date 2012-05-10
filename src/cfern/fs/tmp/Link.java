/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp;

import cfern.sys.str.Stat;

/**
 * A symbolic link
 */
public class Link extends File {
  
  private final String target;
  
  /**
   * Create link to target (may be absolute or relative)
   */
  public Link(String target) {
    this.target = target;
  }
  
  /**
   * Returns this
   */
  public Link link() {
    return this;
  }
  
  /**
   * Returns literal target (may be relative)
   */
  String target() {
    return target;
  }
  
  /**
   * Returns absolute target for given fs/path.
   */
  String target(String mount, String path) {
    if (target.startsWith("/"))
      return target;

    // make target absolute if it is not
    return mount + path.substring(0, path.lastIndexOf("/") + 1) + target;
  }
  
  /**
   * Returns stat of link, not target...
   */
  Stat stat() {
    Stat st = stat(Stat.lnk);
    st.blksize = 0;
    st.blocks = 0;
    st.size = target.length();
    return st;
  }
  
}
