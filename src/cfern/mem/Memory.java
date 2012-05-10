/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.mem;
import java.io.IOException;
import java.util.*;
import cfern.Driver;
import cfern.elf.SymbolTable;
import cfern.fs.FileDesc;

/**
 * An abstract memory class. Maintains useful things associated with the memory,
 * like the symbol table and argument pointers, and higher level methods like
 * array and string load/store. Low level operations delegate to the backing
 * implementation.
 * 
 * Unless otherwise noted, any attempt to access a virtual address that is not
 * mapped will throw a RuntimeException. It is unspecified whether abstract
 * methods allow non unaligned accesses (though the only implementation
 * doesn't).
 * 
 * NOTE: This class are NOT thread safe. Instances must NOT be
 * visible to more than one running thread.
 * 
 * TODO a non shared paged byte memory, and a paged int memory
 * 
 * TODO consider splitting methods that use buf into a subclass
 * e.g. Memory -> BufferedMemory -> ByteMemory 
 * and  Memory -> MetaMemory(Memory)
 */
public abstract class Memory {
  
  /**
   * Stack pointer. Will be lowered when args/env are stored.
   * The MIPS psabi specifies this should be 8 bytes aligned.
   */
  private int sp;
  
  /**
   * List of interesting pointers
   */
  private final ArrayList<Address> addresses = new ArrayList<Address>();
  
  /**
   * Program break (end of initialised data section)
   */
  private int brk;
  
  /**
   * The symbol table. Use getname to pass a address to name request to it.
   */
  private SymbolTable st;
  
  /**
   * Temporary buffer for doing generic file IO (the implementation may not be
   * byte backed or a given address may soon cross a page boundary, etc).
   */
  private final byte[] buf = new byte[16384];
  
  /**
   * Does nothing
   */
  protected Memory() {
    // yawn
  }
  
  /**
   * Copys the generic pointers for this memory.
   */
  protected Memory(Memory other) {
    st = other.st;
    sp = other.sp;
  }
  
  /**
   * Get the stack pointer (only use after storing args/env)
   */
  public int getsp() {
    return sp;
  }
  
  /**
   * Associate name with address (informative use only).
   */
  public void put(String name, int addr) {
    if (addr != 0)
      addresses.add(new Address(addr, name));
  }
  
  /**
   * Create a memory region at given address. Used when loading ELF file as per
   * program headers. Has no affect if region is already created.
   * If addr is 0, then a new upper memory block is allocated and the address returned.
   */
  public abstract int alloc(int a, int size);
  
  /**
   * Allocate the stack area.
   * Only call this once before thread starts running.
   */
  public final void allocstack(int size) {
    sp = stack();
    alloc(stack() - size, size);
  }
  
  /**
   * Get the initial stack pointer (a constant)
   */
  public abstract int stack();
  
  /**
   * Free a block of memory allocated with alloc(size), typically when closing
   * an memory mapped file.
   */
  public abstract void free(int addr);
  
  /**
   * Create a new Memory subclass
   */
  public static final Memory make() {
    // should probably have a factory class in future
    /*
    if (Driver.pagemem)
      return new PagedMemory();
    else if (Driver.intmem)
      return new IntMemory();
    else
      return new ByteMemory();
    */
    return new PagedMemory();
  }
  
  /**
   * Create a copy of this memory (for fork()). The actual backing array may or
   * may not be copied.
   */
  public abstract Memory copy();
  
  /**
   * Completly clear the contents of this memory (e.g. by freeing all pages).
   * Use before reloading memory from ELF file. You must also reallocate the
   * stack.
   */
  public abstract void clear();
  
  /**
   * store args and env in the way glibc expect (see sysdeps/mips/elf/start.S)
   */
  public void store_args_and_env (List<String> args, List<String> env) {
    int[] argp = push_strings(args);
    int[] envp = push_strings(env);
    
    // update sp to be below stored arrays
    sp = (sp - (argp.length * 4) - (envp.length * 4) - 8) & ~0x7;
    
    // store argc
    store_word(sp, args.size());
    
    // store argv
    int p = sp + 4;
    int argpp = p;
    for (int n = 0; n < argp.length; n++, p += 4)
      store_word(p, argp[n]);
    
    // store env
    int envpp = p;
    for (int n = 0; n < envp.length; n++, p += 4)
      store_word(p, envp[n]);
    
    Driver.opt().info("argv: %x env: %x aux: %x", argpp, envpp, 0);
  }
  
  /**
   * Store strings into memory at sp and update sp to be below them.
   * Returns vector of addresses (including terminating null).
   */
  private int[] push_strings (List<String> args) {
    int size = args.size();
    
    // total length of strings including ending nul
    int len = 0;
    for (int n = 0; n < size; n++)
      len = len + args.get(n).length() + 1;
    
    // string table pointer on lower 4 byte boundary
    int args_p = (sp - len) & ~3;
    
    // save string table
    int[] argv = new int[size + 1];
    for (int n = 0, arg_p = args_p; n < size; n++) {
      argv[n] = arg_p;
      arg_p += store_string(arg_p, args.get(n));
    }
    
    // update sp (must be on 8 byte boundary)
    sp = (args_p - 8) & ~7;
    
    return argv;
  }
  
  /**
   * load an array from strings from memory.
   * last char* should be null.
   */
  public final ArrayList<String> load_env(int envp) {
    ArrayList<String> env = new ArrayList<String>();
    int p = envp;
    while (true) {
      int a = load_word(p);
      if (a == 0)
        break;
      String s = load_string(a);
      if (s == null)
        throw new RuntimeException("null env string");
      env.add(s);
      p += 4;
    }
    return env;
  }
  
  /**
   * Set the symbol table (called by elfloader.load())
   */
  public final void setsymbols (SymbolTable st) {
    this.st = st;
  }
  
  /**
   * Set the program break (called by ElfLoader only).
   * After this use brk()
   */
  public final void setbrk(int brk) {
    this.brk = brk;
  }
  
  /**
   * Resize the break (to allocate more memory for the data section).
   */
  public final int brk(int end_p) {
    if (end_p == 0)
      return brk;
    int size = end_p - brk;
    // note programs usually exit with a negative size
    if (size > 0) {
      alloc(brk, size);
    }
    brk = end_p;
    return brk;
  }
  
  /**
   * Returns the address of a symbol from the symbol table.
   * Note: this method is slow because it does a linear search.
   */
  public int getaddress(String name) {
    return st.getaddress(name);
  }
  
  /**
   * Get the name of function at given address from symbol table. Does not
   * include address or offset.
   */
  public String getfunname (int a) {
    return st.getfunname(a);
  }
  
  /**
   * Get the name of given address from symbol table. Returns something like
   * 4004cc&lt;g+2c&gt; or just the hex number if it's not in the symbol table.
   * Never returns null.
   */
  public String getname (int a) {
    // TODO check addresses vector
    if (st != null) {
      String ret = st.getname(a);
      if (ret != null)
        return ret;
    }
    // check some things not in the symbol table
    if (a >= sp)
      return String.format("%x<STACK+%x>", a, a - sp);

    return Integer.toHexString(a);
  }
  
  /**
   * Return true if loading or storing to this address will (probably) not cause
   * an exception. (More precise checking would require access size and length).
   */
  public abstract boolean bound(int addr);
  
  /**
   * Load word from virtual address a. Probably the most popular method in the
   * program. Fortunatly being virtual doesn't seem to slow it.
   */
  public abstract int load_word(int a);
  
  /**
   * Store word at virtual address a
   */
  public abstract void store_word(int a, int x);
  
  /**
   * Store an array of words into memory (must be 8 byte boundary and size
   * multiple of 2).
   */
  public final void store_words(int addr, int[] arr) {
    if ((addr & 7) != 0)
      throw new RuntimeException(String.format("not 8 byte boundary: %x", addr)); 
    if (arr.length % 2 != 0)
      throw new RuntimeException("array must be even length");
    for (int p = addr, n = 0; n < arr.length; n++, p += 4)
      store_word(p, arr[n]);
  }
  
  /**
   * Push an array of words onto a stack (not overwriting word at [sp]).
   * Returns new lower pointer to first word.
   */
  public final int push_words(int sp, int[] arr) {
    int arr_p = sp - (arr.length * 4);
    store_words(arr_p, arr);
    return arr_p;
  }
  
  /**
   * Push an array of words onto the stack.
   * Updates and returns new (lower) stack pointer.
   */
  public final int push_words(int[] arr) {
    sp = sp - (arr.length * 4);
    store_words(sp, arr);
    return sp;
  }
  
  /**
   * Load an array of words.
   * Returns new (higher) pointer to next word after array.
   */
  public final int load_words (int sp, int[] vec) {
    for (int n = 0; n < vec.length; n++, sp += 4)
      vec[n] = load_word(sp);
    //System.err.printf("pop_array(%x,vec[%d]) sp now %x\n", spx, vec.length, sp);
    return sp;
  }
  
  /**
   * Load halfword from virtual address a.
   */
  public abstract short load_half(int a);
  
  /**
   * Store halfword at virtual address a
   */
  public abstract void store_half(int a, short x);
  
  /**
   * Load byte from virtual address a.
   */
  public abstract byte load_byte(int a);
  
  /**
   * Store byte at virtual address a
   */
  public abstract void store_byte(int a, byte x);
  
  /**
   * Load a series of bytes at virtual address a.
   */
  public void load_bytes(int a, byte[] buf, int off, int len) {
    for (int n = 0; n < len; n++)
      buf[off + n] = load_byte(a + n);
  }
  
  /**
   * Store a series of bytes at virtual address a. This is used when reading
   * files, so it helps to override this implementation with something faster.
   */
  public void store_bytes(int a, byte[] buf, int off, int len) {
    for (int n = 0; n < len; n++)
      store_byte(a + n, buf[off + n]);
  }
  
  /**
   * Load null terminated string from virtual address a. Never returns null (use
   * bound() first, but that only checks the first byte; really the program
   * should crash if that fails).
   */
  /*
  public final String load_string (int a) {
    int n = 0;
    byte b;
    while ((b = load_byte(a + n)) != 0)
      buf[n++] = b;
    
    // this is pretty inefficient
    return (n == 0) ? "" : new String(buf, 0, n, Driver.charset);
  }
  */
  
  /**
   * Load null terminated string from virtual address a.
   * Returns null if not bound or too long.
   */
  public final String load_string (int a) {
    if (!bound(a))
      return null;
    
    int n = 0;
    byte b;
    while ((b = load_byte(a + n)) != 0 && n < buf.length)
      buf[n++] = b;
    if (n == 4096) {
      Driver.opt().error("load_string: string more than 16k");
      return null;
    }
    
    // TODO this is pretty inefficient
    return (n == 0) ? "" : new String(buf, 0, n, Driver.charset);
  }
  
  /** 
   * Store a nul terminated string.
   * Return number of bytes written (length+1).
   */
  public final int store_string (int a, String s) {
    byte[] b = s.getBytes(Driver.charset);
    for (int n = 0; n < b.length; n++)
      store_byte(a + n, b[n]);
    store_byte(a + b.length, (byte) 0);
    return b.length + 1;
    /*
    int len = s.length();
    for (int n = 0; n < len; n++)
      store_byte(a + n, (byte) s.charAt(n));
    store_byte(a + len, (byte) 0);
    return len + 1;
    */
  }
  
  /**
   * Load a double word.
   * Not really sure if this is correct
   */
  public long load_dword (int a) {
    long m = 0xffffffff;
    long d = ((load_word(a) & m) << 32) | (load_word(a + 4) & m);
    return d;
  }
  
  /**
   * Store a double word.
   * Not really sure if this is correct.
   */
  public void store_dword (int a, long d) {
    long m = 0xffffffff;
    store_word(a, (int) ((d >>> 32) & m)); // m might not be necessary
    store_word(a + 4, (int) (d & m));
  }
  
  /**
   * Load the indexes (in order) of the set bits for array of len bytes
   * (accessed as words). If no bits are set, null is returned.
   */
  public short[] load_bitset (int a, int len) {
    if ((len * 8) > Short.MAX_VALUE)
      throw new RuntimeException("bit set too big: " + len);
    
    int set = 0;
    for (int n = 0; n < len; n += 4)
        set += Integer.bitCount(load_word(a + n));
    //Driver.opt().info("load_bitset: %d bits set", set);
    if (set == 0)
      return null;
    
    short[] ret = new short[set];
    for (int n = 0, p = 0; n < len && p < set; n += 4) {
      int w = load_word(a + n);
      for (int b = 0; w != 0; w >>>= 1, b++)
        if ((w & 1) == 1)
          ret[p++] = (short) ((n * 8) + b);
    }
    
    //Driver.opt().info("load_bitset: returning %s", Arrays.toString(ret));
    return ret;
  }
  
  /**
   * Set the bits indexed in "bits" to true.
   * Bits must be >= 0 and in order.
   * If bits is null, then no bits are considered set.
   * Returns number of bits set.
   */
  public int store_bitset (int a, int len, short[] bits) {
    // n: word number, bits[p]: next bit, b: current bit, s: shift amount (b%32)
    for (int n = 0, p = 0, b = 0; n < len; n += 4) {
      // e.g. [0, 2, 8, 123]
      int w = 0;
      if (bits != null) {
        for (int s = 0; p < bits.length && s < 32; s++, b++) {
          if (bits[p] == b) {
            w |= (1 << s);
            p++;
          }
        }
      }
      store_word(a + n, w);
    }
    return bits == null ? 0 : bits.length;
  }
  
  /**
   * Read from a file and write to memory.
   * Returns number of bytes read, 0 on end of file.
   * FIXME should throw IOException, also needs tidying
   */
  
  public int read_from (FileDesc f, int addr, int buflen) {
    int ret = 0;
    try {
      int len;
      // need to loop in case buf is too short
      do {
        len = f.read(buf, 0, Math.min(buf.length, buflen - ret));
        if (len > 0) {
          store_bytes(addr + ret, buf, 0, len);
          ret += len;
        }
      } while (len > 0 && ret < buflen && f.inChannel() == null && f.available() > 0);
    } catch (IOException e) {
      // FIXME read should return EIO
      throw new RuntimeException("error reading from " + f, e);
    }
    return ret < 0 ? 0 : ret;
  }
  
  /**
   * Copies a sequence of bytes from memory to the specified file
   */
  public int write_to(FileDesc f, int buf_p, int buflen) {
    try {
      int i = 0;
      for (int n = 0; n < buflen; n++) {
        if (i == buf.length) {
          f.write(buf, 0, buf.length);
          i = 0;
        }
        // FIXME use load_bytes as it may be in the subclass
        buf[i++] = load_byte(buf_p + n);
      }
      f.write(buf, 0, i);
    } catch (IOException e) {
      // FIXME write needs to return ENOSPC.. or something
      // or just number of bytes..
      throw new RuntimeException(e);
    }
    return buflen;
  }

  /**
   * Memory map a file (badly).
   */
  public int map(FileDesc file, int off, int len, boolean share) {
    int a = alloc(0, len);
    Driver.opt().info("Memory: map(%s,%d,%d,...) = %s\n", file, off, len, getname(a)); 
    int pos = file.offset();
    file.seekset(off);
    read_from(file, a, len);
    file.seekset(pos);
    return a; 
  }

  /**
   * Set len bytes to b, return from.
   */
  public int memset(int from, byte b, int len) {
    for (int n = 0; n < len; n++)
      store_byte(from + n, b);
    return from;
  }
  
  /**
   * Returns the various memory pointers
   */
  public String toString() {
    return String.format("brk=%-8x b sp=%-8x addrs=%s", brk, sp, addresses);
  }
  
  /**
   * Exception for invalid memory addresses
   */
  class SegFault extends RuntimeException {
    /**
     * Create a new segfault for given address and underlying cause
     */
    SegFault(int a, Exception e) {
      super(String.format("invalid address %x", a), e);
    }
  }
  
  /**
   * Represents a name for an address in the memory
   */
  private class Address implements Comparable<Address> {
    final int addr;
    final String name;
    Address(int addr, String name) {
      this.addr = addr;
      this.name = name;
    }
    public int compareTo(Address other) {
      return addr - other.addr;
    }
  }
  
} // end of class Memory
