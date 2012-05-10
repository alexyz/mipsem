/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;

import cfern.fs.MyDataInput;

/**
 * An ELF 32bit section header. You know you love 'em
 */
final class Elf32_Shdr {
  
  /**
   * Section types
   */
  static final byte
    SHT_PROGBITS = 1,
    SHT_SYMTAB = 2,
    SHT_STRTAB = 3,
    SHT_RELA = 4,
    SHT_DYNAMIC = 6,
    SHT_NOTE = 7,
    SHT_NOBITS = 8,
    SHT_REL = 9;
  
  /** a mips elf32_reginfo table */
  static final int SHT_MIPS_REGINFO = 0x70000006;
  
  /** section should be writeable at run time */
  static final int SHF_WRITE = 1;
  /** section occupies memory at run time */
  static final int SHF_ALLOC = 2;
  /** section contains code */
  static final int SHF_EXECINSTR = 4;
  
  /** index of name in string table */
  final int sh_name;
  /** type, e.g. symbol table, reginfo, or relocation entries */
  final int sh_type;
  /** section should be allocated/writeable/executable */
  final int sh_flags;
  /** address this section should be loaded at */
  final int sh_addr;
  /** offset in file of this section */
  final int sh_offset;
  /** size of this section in the file */
  final int sh_size;
  final int sh_link;
  final int sh_info;
  final int sh_addralign;
  /** size of each record in the section */
  final int sh_entsize;
  /** the name as loaded from the string table */
  String name;
  
  /**
   * Read section header from given DataInput
   */
  public Elf32_Shdr(MyDataInput f) throws IOException {
    sh_name = f.readint();
    sh_type = f.readint();
    sh_flags = f.readint();
    sh_addr = f.readint();
    sh_offset = f.readint();
    sh_size = f.readint();
    sh_link = f.readint();
    sh_info = f.readint();
    sh_addralign = f.readint();
    sh_entsize = f.readint();
  }
  
  String type() {
    switch (sh_type) {
      case SHT_RELA: return "rela";
      case SHT_DYNAMIC: return "dynamic";
      case SHT_NOTE: return "note";
      case SHT_PROGBITS: return "progbits";
      case SHT_SYMTAB: return "symtab";
      case SHT_STRTAB: return "strtab";
      case SHT_NOBITS: return "nobits";
      case SHT_REL: return "rel";
      case SHT_MIPS_REGINFO: return "reginfo";
    }
    return Integer.toHexString(sh_type);
  }

  /**
   * Return string describing section
   */
  public String toString() {
    String f1 = ((sh_flags & SHF_WRITE) != 0) ? "write" : "";
    String f2 = ((sh_flags & SHF_EXECINSTR) != 0) ? "exec" : "";
    String f3 = ((sh_flags & SHF_ALLOC) != 0) ? "alloc" : "";
    String s = 
      "|  %-14s  %-14s %s %s %s\n" +
      "|      flag: %8x  addr: %8x   off: %8x     size: %d\n" +
      "|      link: %8x  info: %8x  algn: %8x  entsize: %d";
    
    return String.format(s, name, type(), f1, f2, f3, sh_flags, sh_addr, sh_offset, sh_size, sh_link, sh_info, sh_addralign, sh_entsize);
  }
  
} // end of class Elf32_Shdr
