/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;

import cfern.fs.MyDataInput;

/**
 * ELF header file, almost straight from TIS ELF specification. Loads from given
 * DataInput and provides useful toString. Fields are public for the sake of
 * simplicity, but should not be written to.
 */
final class Elf32_Ehdr {
  
  public static final int ET_NONE = 0;
  public static final int ET_REL = 1;
  /** executable file */
  public static final int ET_EXEC = 2;
  /** shared object file */
  public static final int ET_DYN = 3;
  public static final int ET_CORE = 4;
  public static final int ET_LOPROC = 0xff00;
  public static final int ET_HIPROC = 0xffff;
  
  /** type (2 = executable) */
  final short e_type; 
  /** machine type (8 = mips-i) */
  final short e_machine; 
  final int e_version;
  /** entry point of program (starting pc) */
  final int e_entry;
  /** offset of first program header table in file */
  final int e_phoff;
  /** offset of first section header table in file */
  final int e_shoff;
  final int e_flags;
  final short e_ehsize;
  final short e_phentsize;
  /** number of program header table entries */
  final short e_phnum; 
  final short e_shentsize;
  /** number of section header table entries */
  final short e_shnum; 
  /** index number of the section header describing the string table */
  final short e_shstrndx;
  /** these appear at the start of every elf file */
  static final int ELF_MAGIC = 0x7f454c46;
  
  /** load elf header from file */
  Elf32_Ehdr(MyDataInput f) throws IOException {
    int e_ident = f.readint(); // 4
    if (e_ident != ELF_MAGIC)
      throw new IOException("Not an ELF file");
    f.seekcur(12); // 16
    e_type = f.readshort(); // 18
    e_machine = f.readshort(); // 20
    e_version = f.readint(); // 24
    e_entry = f.readint(); // 28
    e_phoff = f.readint(); // 32
    e_shoff = f.readint(); // 36
    e_flags = f.readint(); // 40
    e_ehsize = f.readshort(); // 42
    e_phentsize = f.readshort(); // 44
    e_phnum = f.readshort(); // 46
    e_shentsize = f.readshort(); // 48
    e_shnum = f.readshort(); // 50
    e_shstrndx = f.readshort(); // 54
  }
  
  private String type() {
    switch (e_type) {
      case ET_CORE: return "core";
      case ET_DYN: return "shared";
      case ET_EXEC: return "exec";
      case ET_REL: return "reloc";
      default: return Integer.toString(e_type);
    }
  }
  
  /**
   * Print a description of this ELF header
   */
  public String toString() {
    String s = "| type: %-8s machine: %-4x version: %-10d ehsize: %-10d\n" +
            "| entry: %-8x flags: %-8x str: %-8x\n" +
            "| phoff: %-8x phent: %-4d phnum: %-4d\n" +
            "| shoff: %-8x shent: %-4d shnum: %-4d";
    return String.format(s, type(), e_machine, e_version, e_ehsize, e_entry, e_flags, e_shstrndx,
        e_phoff, e_phentsize, e_phnum, e_shoff, e_shentsize, e_shnum);
  }
  
} // end of class Elf32_Ehdr
