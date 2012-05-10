/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;

import cfern.fs.MyDataInput;

/**
 * An ELF program header. Most files contain about 4-8
 */
final class Elf32_Phdr {
  
  static final int PT_NULL = 0;
  /** a block to load */
  static final int PT_LOAD = 1;
  static final int PT_DYNAMIC = 2;
  /** Path to interpreter (dynamic linker, ld-linux) */
  static final int PT_INTERP = 3;
  static final int PT_NOTE = 4;
  static final int PT_SHLIB = 5;
  static final int PT_PHDR = 6;
  static final int PT_LOPROC = 0x70000000;
  static final int PT_HIPROC = 0x7fffffff;
  /**
   * register usage for shared object, mips psabi page 86
   */
  static final int PT_MIPS_REGINFO = 0x70000000;
  
  /** Load if equal to PT_LOAD */
  final int p_type;
  /** Offset of section in file */
  final int p_offset; 
  /** Where to load in memory */
  final int p_vaddr; 
  final int p_paddr;
  /** Length in file of section */
  final int p_filesz;
  /** Length in memory (greater than or equal to filesz) */
  final int p_memsz; 
  /** 1 = exec, 2 = write, 4 = read */
  final int p_flags;
  final int p_align;
  
  /**
   * Load program header from given DataInput
   */
  public Elf32_Phdr(MyDataInput f) throws IOException {
    p_type = f.readint();
    p_offset = f.readint();
    p_vaddr = f.readint();
    p_paddr = f.readint();
    p_filesz = f.readint();
    p_memsz = f.readint();
    p_flags = f.readint();
    p_align = f.readint();
  }
  
  /**
   * Return string of the type of this program header
   */
  public String type() {
    switch (p_type) {
      case PT_NULL: return "null";
      case PT_LOAD: return "load";
      case PT_DYNAMIC: return "dynamic";
      case PT_INTERP: return "interp";
      case PT_NOTE: return "note";
      case PT_SHLIB: return "shlib";
      case PT_PHDR: return "phdr";
      case PT_MIPS_REGINFO: return "reginfo";
    }
    return Integer.toHexString(p_type);
  }
  
  /**
   * Print a full description of this header
   */
  public String toString() {
    String f = "| type: %-8s  offset: %-8s\n" +
            "| vaddr: %-8x  paddr: %-8x\n" +
            "| filesz: %d  memsize: %d  flags: %x  align: %d";
    return String.format(f, type(), p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_flags, p_align);
  }
  
} // end of class Elf32_Phdr
