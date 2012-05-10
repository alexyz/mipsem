/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;
import java.util.*;
import cfern.fs.MyDataInput;

/**
 * Loads an array of symbol table objects from an ELF file. Automatically
 * removes useless entries and sorts by address. If a program is relocated, then
 * use setloadaddr().
 */
public class SymbolTable {

	private final Elf32_Sym[] syms;
	private final Elf32_Sym[] badsyms;
	/**
	 * The offset this file was loaded at
	 */
	private int loadoff = 0;

	/**
	 * Create a valid SymbolTable but with no symbols
	 */
	public SymbolTable() {
		syms = null;
		badsyms = null;
	}

	/**
	 * Creates a symbol table by loading the Elf32_Sym array from the given ELF
	 * file. Needs string table to load names, section headers to load section
	 * names and index of the symbol table section.
	 */
	public SymbolTable(MyDataInput f, byte[] strtab, Elf32_Shdr[] shdrs, int index) throws IOException {

		int numsyms = shdrs[index].sh_size / shdrs[index].sh_entsize;

		ArrayList<Elf32_Sym> symbol_list = new ArrayList<Elf32_Sym>(numsyms);

		for (int n = 0; n < numsyms; n++) {
			Elf32_Sym s = new Elf32_Sym(f, strtab, shdrs);
			if (s.rank() > 0)
				symbol_list.add(s);
		}

		Collections.sort(symbol_list);
		ArrayList<Elf32_Sym> dropped = new ArrayList<Elf32_Sym>();

		// remove symbols with equal addresses
		int n = 0, prev_value = 0;
		while (n < symbol_list.size()) {
			Elf32_Sym s = symbol_list.get(n);
			if (prev_value == s.st_value) {
				dropped.add(symbol_list.remove(n));
			} else {
				// go to next symbol
				prev_value = s.st_value;
				n++;
			}
		}

		syms = symbol_list.toArray(new Elf32_Sym[symbol_list.size()]);
		badsyms = dropped.toArray(new Elf32_Sym[dropped.size()]);
		//System.err.printf("# loaded %d symbols\n", symbol_list.size());
	}

	/**
	 * find the symbol table index for an address. returns -1 on err. used as the
	 * back end to getnamebyaddr and getobjbyaddr. may not work for addresses >
	 * 2gb...
	 * TODO should use Arrays.binarySearch(int[])
	 */
	private int getnameindex(int target) {
		if (syms == null)
			return -1;

		int first = syms[0].st_value;
		int last = syms[syms.length-1].st_value;
		if ((target < first) || (target > last))
			return -1;

		// binary search of symbol array for biggest address <= target
		// crashes if target < first

		int top = syms.length - 1;
		int mid = top / 2;
		int bot = 0;

		while (top > bot) {
			int val = syms[mid].st_value;
			if (val == target)
				// if we get lucky. not actually necessary
				return mid;
			if (val > target) {
				// we are too high. target is between bot and mid-1
				top = mid - 1;
			} else {
				// too low. target is between mid+1 and top
				bot = mid + 1;
			}
			mid = ((top - bot) / 2) + bot;
		}

		// the search either gives the right answer or the next one up. so check here.
		if (syms[mid].st_value > target)
			mid--;
		return mid;
	}

	/**
	 * Set the load address of this elf section
	 */
	public void setloadoff(int loadoff) {
		this.loadoff = loadoff;
	}

	/**
	 * get the name of an address and the offset.
	 * returns null if it can't reasonably find it
	 */
	public String getname (int target) {
		// total hack
		int ntarget = target - loadoff;

		int mid = getnameindex(ntarget);
		if (mid == -1)
			return null;
		int off = ntarget - syms[mid].st_value;
		if (off == 0)
			return String.format("%x<%s>", target, syms[mid].name);
		if (off < 0x10000)
			return String.format("%x<%s+%x>", target, syms[mid].name, off);
		return null;
	}

	public String getnamesafe (int target) {
		int ntarget = target - loadoff;
		String name = getname(ntarget);
		return name != null ? name : Integer.toHexString(target);
	}

	/**
	 * Get name of address, no offset (for function log)
	 */
	public String getfunname(int target) {
		int ntarget = target - loadoff;
		int mid = getnameindex(ntarget);
		if (mid < 0)
			return Integer.toHexString(target);
		int off = ntarget - syms[mid].st_value;
		if (off <= 0x2000)
			return syms[mid].name;
		return Integer.toHexString(target);
	}

	/**
	 * Get the address of a symbol. This method is O(n), i.e. very slow. Use only
	 * for debugging purposes.
	 */
	public int getaddress (String name) {
		if (syms == null) {
			return Integer.parseInt(name, 16);
		}
		for (int n = 0; n < syms.length; n++) {
			Elf32_Sym s = syms[n];
			if (s.name.equals(name))
				return s.st_value;
		}
		return 0;
	}

	/**
	 * Returns the entire symbol table. ElfLoader.main() calls this
	 */
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Dropped symbols:\n");
		for (int n = 0; n < badsyms.length; n++)
			s.append(badsyms[n]).append("\n");
		s.append("Symbols:\n");
		for (int n = 0; n < syms.length; n++)
			s.append(syms[n]).append("\n");
		return s.toString();
	}

} // end of class SymbolTable
