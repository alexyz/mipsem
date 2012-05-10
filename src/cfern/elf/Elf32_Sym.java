/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 * http://www.alexslack.dsl.pipex.com/
 */

package cfern.elf;
import java.io.*;

import cfern.fs.MyDataInput;

/**
 * An elf symbol object. Probably a bit inefficient. Is sorted in an array by
 * SymbolTable
 */
class Elf32_Sym implements Comparable<Elf32_Sym> {
  
  /** symbol table types */
  static final int
  STT_NOTYPE = 0,
  STT_OBJECT = 1,
  STT_FUNC = 2,
  STT_SECTION = 3,
  STT_FILE = 4;
  
  /** symbol binding */
  static final int
  STB_LOCAL = 0,
  STB_GLOBAL = 1,
  STB_WEAK = 2;
  
  /** symbol name and section name */
  final String name, secname;
  
  final int st_name, st_size;
  /** address of object that symbol refers to */
  final int st_value;
  /** symbol type (can be misleading) */
  final byte st_info;
  final byte st_other;
  /** refers to section */
  final short st_shndx;
  
  /**
   * Load a symbol from the given DataInput with reference to the given string
   * table.
   */
  Elf32_Sym(MyDataInput f, byte[] strtab, Elf32_Shdr[] shdrs) throws IOException {
    st_name = f.readint();
    st_value = f.readint();
    st_size = f.readint();
    st_info = f.read(); 
    st_other = f.read();
    st_shndx = f.readshort(); 
    name = ElfLoader.load_string(strtab, st_name);
    secname = st_shndx == -15 ? "abs" : shdrs[st_shndx].name;
  }
  
  /**
   * Calculate the 'importance' of a symbol (used when sorting the symbol table)
   */
  int rank() {
    if ((st_value == 0) || (name.length() == 0))
      return 0;
    
    // ignore absolute symbols, these to not appear to be real memory addresses
    if (st_shndx == -15)
      return 0;
    
    // at least it has an address and a name
    int ret = 1;
    
    // has a non annoying name
    if (!name.startsWith("_"))
      ret++;
    
    // is not a hidden symbol
    if (st_other == 0)
      ret += 2;
    
    // visibility bonus
    int stb = st_info >>> 4;
    if (stb == STB_GLOBAL)
      ret += 2;
    
    // code bonus
    int stt = st_info & 0xf;
    if (stt == STT_OBJECT || stt == STT_FUNC)
      ret += 3;
    
    return ret;
  }
  
  /**
   * Lowest address first, then most important first
   */
  public int compareTo(Elf32_Sym s) {
    int a = st_value - s.st_value;
    if (a != 0)
      return a;
    return s.rank() - rank();
  }
  
  /**
   * Return the type of this symbol
   */
  String type() {
    int stt = st_info & 0xf;
    switch (stt) {
      case STT_NOTYPE: return "notype";
      case STT_OBJECT: return "var/obj";
      case STT_FUNC: return "function";
      case STT_SECTION: return "section";
      case STT_FILE: return "file";
    }
    return Integer.toHexString(stt);
  }
  
  /**
   * Return the binding for this symbol
   */
  String bind() {
    int stb = st_info >>> 4;
    switch (stb) {
      case STB_LOCAL: return "local ";
      case STB_GLOBAL: return "global";
      case STB_WEAK: return "weak";
    }
    return Integer.toHexString(stb);
  }
  
  String other() {
    switch (st_other) {
      case 0: return "def";
      case 2: return "hide";
    }
    return Integer.toString(st_other);
  }
  
  /**
   * Print a full description of this symbol
   */
  public String toString() {
    return String.format("%8x %-6s %-8s %-4s %d: %-10s %s", st_value, bind(), type(), other(), rank(), secname, name);
  }
  
} // end of class Elf32_Sym
