/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.mem;

import cfern.Driver;

/**
 * A shared paged byte memory. 
 * TODO make an unshared paged byte memory
 * 0-4 mb: unmapped. 
 * 4-256mb: program. 
 * 256mb: data section
 * 384mb: stack (grows down)
 * 384mb: mmap blocks.
 * 512mb: top.
 */
final class PagedMemory extends Memory {

	/**
	 * Initial stack pointer, 384mb.
	 */
	private static final int stack = 0x18000000;

	/**
	 * Bytes in a page, 64k. Actual pages are 4 bytes longer, for the shared and
	 * number allocated flags.
	 */
	private static final int pagesize = 0x10000;

	/**
	 * Number of pages, 8k
	 */
	private static final int numpages = 8192;

	/**
	 * 8192 pages of 65536 bytes = 512mb mem
	 */
	private final byte[][] pages = new byte[numpages][];

	/**
	 * Create a paged byte memory with no regions.
	 */
	protected PagedMemory() {
		super();
		// don't create stack until clear() is called before loading elf
	}

	/**
	 * share all pages when copying
	 */
	private PagedMemory(PagedMemory other) {
		super(other);
		for (int n = 0; n < pages.length; n++) {
			byte[] mem = other.pages[n];
			if (mem != null) {
				Pages.incShared(mem);
				pages[n] = mem;
			}
		}
	}

	public Memory copy() {
		return new PagedMemory(this);
	}

	/**
	 * Create memory regions for the given addresses.
	 * Used when loading elf files.
	 * TODO should allocate somewhere new for fixed mappings already mapped (ref. the DIRECT flag to mmap)
	 */
	public int alloc(int addr, int size) {
		if (addr < pagesize)
			return alloc(size);

		int startp = addr >> 16;
		int endp = (addr + size - 1) >> 16;
		int nump = endp - startp;

		if (addr > stack)
			Driver.opt().elflog("PagedMemory: fixed alloc of mmap pages %d to %d", startp, endp); 

		for (int p = startp; p <= endp; p++) {
			byte[] mem = pages[p];
			if (mem == null)
				pages[p] = mem = Pages.alloc();
			if (addr > stack && p == startp)
				Pages.setAuxByte(mem, 0, nump);
		}
		return addr;
	}

	/**
	 * Allocate upper memory for mmap
	 */
	private int alloc(int size) {
		int sizep = (size / pagesize) + ((size % pagesize) > 0 ? 1 : 0);
		//System.err.printf("# alloc %d bytes (%d pages)\n", size, sizep);
		if (sizep > 100)
			throw new RuntimeException("cannot alloc " + sizep + " pages");
		// starting page of alloc
		int startp = -1;
		for (int p = stack >>> 16; p < pages.length; p++) {
			byte[] mem = pages[p];
			if (mem != null) {
				startp = -1;
				continue;
			}
			if (startp == -1)
				startp = p;
			if ((p - startp + 1) == sizep) {
				for (int n = 0; n < sizep; n++)
					//newpage(startp + n);
					pages[startp + n] = Pages.alloc();
				Pages.setAuxByte(pages[startp], 0, sizep);
				//System.err.printf("# allocated %d pages at %s\n", sizep, getname(startp << 16));
				return startp << 16;
			}
		}
		throw new RuntimeException("could not find " + sizep + " free pages");
	}

	/**
	 * Drop all pages (used during execve and at exit)
	 */
	public void clear() {
		for (int n = 0; n < pages.length; n++) {
			byte[] page = pages[n];
			if (page != null) {
				Pages.free(page);
				pages[n] = null;
			}
		}
	}

	/**
	 * Inital stack pointer, 384mb
	 */
	public int stack() {
		return stack;
	}

	/**
	 * Drop all pages and reallocate stack (used during execve)
	 */
	/*
  public void clear(int stacksize) {
    clear();
    int pages = (stacksize / pagesize) + (stacksize % pagesize != 0 ? 1 : 0);
    allocstack(stack, pages * pagesize);
  }
	 */

	/**
	 * unshare a page
	 */
	private byte[] copypage(int p) {
		// FIXME does not copy pages allocated
		byte[] mem = Pages.alloc(pages[p]);
		//byte[] mem = pages[p].clone();
		//mem[share_flag] = 0;
		pages[p] = mem;
		return mem;
	}

	/**
	 * Free a mmap memory block
	 */
	public void free(int a) {
		int p = a >>> 16;
				int sizep = Pages.getAuxByte(pages[p], 0);
				if (sizep == 0)
					throw new RuntimeException("page " + p + " freed but was not allocated");
				for (int n = p; n < sizep; n++) {
					Pages.free(pages[p + n]);
					pages[p + n] = null;
				}
	}

	public boolean bound(int a) {
		int p = a >> 16;
				// is negative for addresses over 2gb
				if (p < 0 || p >= pages.length)
					return false;
				else
					return pages[p] != null;
	}

	public final int load_word(int a) {
		try {
			byte[] mem = pages[a >> 16];
			a &= 0xffff;
			// TODO make this a field
			int bm = 0xff;
			return ((mem[a] & bm) << 24) | ((mem[a + 1] & bm) << 16) | ((mem[a + 2] & bm) << 8) | (mem[a + 3] & bm);
		} catch (RuntimeException e) {
			// TODO should be in every method
			throw new SegFault(a, e);
		}
	}

	public final void store_word(int a, int x) {
		byte[] mem = pages[a >> 16];
		// TODO the default case should fall through, check the bytecode
		if (Pages.isShared(mem))
			mem = copypage(a >> 16);
		a &= 0xffff;
		mem[a] = (byte) (x >> 24);
		mem[a+1] = (byte) (x >> 16);
		mem[a+2] = (byte) (x >> 8);
		mem[a+3] = (byte) x;
	}

	public final short load_half(int a) {
		byte[] mem = pages[a >> 16];
		a &= 0xffff;
		int bm = 0xff;
		return (short) (((mem[a] & bm) << 8) | (mem[a + 1] & bm));  
	}

	public final void store_half(int a, short x) {
		byte[] mem = pages[a >> 16];
		if (Pages.isShared(mem))
			mem = copypage(a >> 16);
		a &= 0xffff;
		mem[a] = (byte) (x >> 8);
		mem[a + 1] = (byte) x;
	}

	public final byte load_byte(int a) {
		return pages[a >> 16][a & 0xffff];
	}

	public final void store_byte(int a, byte x) {
		byte[] mem = pages[a >> 16];
		if (Pages.isShared(mem))
			mem = copypage(a >> 16);
		mem[a & 0xffff] = x;
	}

	/**
	 * Store a series of bytes at virtual address a
	 */
	public void store_bytes(int addr, byte[] buf, int off, int len) {
		//System.err.printf("store_bytes(%x,buf[%d],%d,%d)\n", addr, buf.length, off, len);

		// first page
		int startp = addr >> 16;
		// number of pages - 1
		int nump = ((addr + len) >> 16) - startp;
		// end address in last page + 1
		int enda = ((addr + len) & 0xffff);
		if (enda == 0)
			// nothing to copy in last page
			nump--;
		// source index
		int si = off;

		for (int p = 0; p <= nump; p++, si += pagesize) {
			// TODO better to break here if p==nump and enda==0?
			int di = p == 0 ? (addr & 0xffff) : 0;
			// enda of 0 means copy whole page
			// (as it's really the second to last page, as per above)
			int ei = p == nump && enda > 0 ? enda : pagesize;
			byte[] mem = pages[startp + p];
			if (Pages.isShared(mem))
				mem = copypage(startp + p);
			//System.err.printf("arraycopy(buf[%d], %d, mem[%d], %d, %d ) p %d nump %d ei %d enda %d\n", 
			//buf.length, si, mem.length, di, ei - di, p, nump, ei, enda);
			System.arraycopy(buf, si, mem, di, ei - di);
		}
	}

	/**
	 * Return a list of mapped pages
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append("\n");
		int nump = 0;
		for (int p = 0; p < pages.length; p++) {
			byte[] mem = pages[p];
			if (mem != null) {
				boolean sh = Pages.isShared(mem);
				int al = Pages.getAuxByte(mem, 0);
				sb.append(String.format("%4x: share: %s alloc: %d\n", p, sh, al));
				nump++;
			}
		}
		sb.append(String.format("Total: %d pages\n", nump));
		return sb.toString();
	}

} // end of class PagedMemory
