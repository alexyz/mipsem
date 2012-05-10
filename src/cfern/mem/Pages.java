package cfern.mem;
import java.util.*;

import cfern.Driver;

/**
 * A 64k+4 byte array allocator. The extra 4 bytes are for the shared count and
 * aux flags, so these must not be overwritten for shared pages.
 * 
 * Non shared pages are read/write but must only be referenced from one thread.
 * Shared pages may be referenced from multiple threads but must be treated as
 * read only. Use alloc(byte[]) to create a private page from a shared one. Use
 * free(byte[]) to add a private page back to the free list (shared pages are
 * left for the garbage collector).
 * 
 * In future could have a map of page lists for different size pages
 */
public final class Pages {

  /**
   * Bytes in a page, 64k. Actual pages are numAux (4) bytes longer, for the
   * shared and number allocated flags.
   */
  public static final int size = 0x10000;
  
  /**
   * Whether this page is shared (page[] offset). Once shared, a page must not
   * be written to, instead it must be copied using alloc(page)
   */
  private static final int shareIndex = size + 0;
  
  /**
   * Index for aux use and number of aux indexes including shareIndex
   */
  private static final int auxIndex = size + 1, auxSize = 4;

  /**
   * List of free pages
   */
  private static final Deque<byte[]> pages = new LinkedList<byte[]>();
  
  /**
   * Count of number of new arrays
   */
  private static int allocated = 0;
  
  private Pages() {
    // private to prevent javadoc
  }
  
  /**
   * Return a new or reused unshared memory page
   */
  public static byte[] alloc() {
    return alloc(null);
  }

  /**
   * Return a new or reused unshared memory page that is a copy of the given
   * page.
   */
  public synchronized static byte[] alloc(byte[] old) {
    if (old != null && old.length != size + auxSize)
      throw new RuntimeException("cannot allocate");
    
    byte[] ret = pages.poll();
    if (ret == null) {
      allocated++;
      ret = new byte[size + auxSize];
    } else if (old == null) {
      // zero reused page if we are not copying (new page is already 0)
      Arrays.fill(ret, (byte) 0);
    }
    
    if (old != null) {
      // copy old page
      System.arraycopy(old, 0, ret, 0, ret.length);
      ret[shareIndex] = 0;
    }
    
    return ret;
  }
  
  /**
   * Free a memory page for reuse (if not shared) or decrement share count.
   */
  public synchronized static void free(byte[] page) {
    free(page, false);
  }

  /**
   * Free a memory page. If ignoreShared is true, then the shared count is
   * ignored (assumed to be private).
   */
  public synchronized static void free(byte[] page, boolean ignoreShared) {
    if (page.length != size + auxSize)
      throw new RuntimeException("not a page");
    if (pages.contains(page))
      throw new RuntimeException("already free");
    
    if (pages.size() > 128) {
      // too many pages, leave it for garbage collector
      Driver.opt().warn("excessive number of free pages: %d", pages.size());
      return;
    }
    
    if (ignoreShared || !isShared(page)) {
      // XXX try and cause errors if it's actually still used, not necessary
      Arrays.fill(page, (byte) -128);
      pages.add(page);
    } else  {
      // either still shared or too many shares (left for gc)
      decShared(page);
    }
  }
  
  /**
   * Return true if a page is shared. If not shared, it cannot become shared
   * without us knowing because only the current thread should have a reference
   * to it.
   * 
   * This method must be fast as it is called before every write to memory
   */
  public static boolean isShared(byte[] page) {
    // doesn't need to be synchronized.. i think
    return page[shareIndex] != 0;
  }
  
  /**
   * Set the given page shared if it is not already. After this it may be shared
   * with other threads.
   */
  public static void incShared(byte[] page) {
    synchronized (page) {
      // synchronize in case it's already shared and some other thread is writing
      if (page[shareIndex] < 127)
        page[shareIndex]++;
    }
  }
  
  /**
   * Decrement the shared count of the page.
   * Returns true if it is no longer used by any thread
   */
  private static void decShared(byte[] page) {
    synchronized (page) {
      int share = page[shareIndex];
      if (share <= 0)
        throw new RuntimeException("not shared");
      // decrement share only if not shared too many times
      if (share < 127)
          page[shareIndex]--;
    }
  }
  
  /**
   * Set aux byte, e.g. the mmap pages allocated count.
   */
  public static void setAuxByte(byte[] page, int index, int num) {
    if (index < 0 || index > 3)
      throw new RuntimeException("no such aux index " + index);
    if (isShared(page))
      throw new RuntimeException("cannot write to shared page");
    if (num < -128 || num > 127)
      throw new RuntimeException("cannot set aux byte to " + num);
    
    page[auxIndex + index] = (byte) num;
  }
  
  /**
   * Get aux byte, e.g. the mmap pages allocated count.
   */
  public static int getAuxByte(byte[] page, int index) {
    if (index < 0 || index > 3)
      throw new RuntimeException("no such aux index " + index);
    return page[auxIndex + index];
  }
  
  /**
   * Get a string describing number of pages allocated
   */
  public static String string() {
    return String.format("pages allocated=%d free=%d", 
        allocated, pages.size());
  }

}
