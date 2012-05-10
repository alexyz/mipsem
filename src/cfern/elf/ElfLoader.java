/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;

import cfern.Driver;
import cfern.fs.*;
import cfern.mem.*;

/**
 * Represents an ELF file, can load all its sections into a memory image. Has
 * useful toString() and main() also (similar to GNU Objdump).
 * 
 * TODO an elf cache, both headers and data by taking a copy of the Memory. files
 * can be uniquely identified by dev+inode
 * 
 * TODO seperate ElfLoader and ELF, make SymbolTable immutable again
 */
public final class ElfLoader {
  
  /**
   * Aux vector constants, see linux/include/linux/auxvec.h and MIPS psABI
   * figure 3-28.
   */
  public static final int AT_NULL = 0, AT_IGNORE = 1, AT_EXECFD = 2, AT_PHDR = 3, AT_PHENT = 4,
      AT_PHNUM = 5, AT_PAGESZ = 6, AT_BASE = 7, AT_FLAGS = 8, AT_ENTRY = 9, AT_NOTELF = 10,
      AT_UID = 11, AT_EUID = 12, AT_GID = 13, AT_EGID = 14, AT_PLATFORM = 15, AT_HWCAP = 16,
      AT_CLKTCK = 17, AT_SECURE = 23;
  
  /**
   * The ELF file, backed by either RandomAccessFile or UnixFile
   */
  private MyDataInput file;
  
  /**
   * The ELF header
   */
  private Elf32_Ehdr ehdr;
  
  /**
   * The section headers
   */
  private Elf32_Shdr[] shdrs;
  
  /**
   * The program headers
   */
  private Elf32_Phdr[] phdrs;
  
  /**
   * Relocation entries (not actually needed)
   */
  private Elf32_Rela[] rela;
  
  /**
   * The symbol array in a convienient object
   */
  private SymbolTable st;
  
  /**
   * General pointer from the strange RegInfo section.
   */
  private int gp;
  
  /**
   * The load address of the first program header. Non zero if loading ld.so.
   */
  private int loadaddr = 0;
  
  /**
   * Load an ELF file from given MyDataInput.
   */
  public static ElfLoader loadelf(MyDataInput in) throws IOException {
    in.seekset(0);
    int magic = in.readint();
    if (magic == Elf32_Ehdr.ELF_MAGIC) {
      in.seekset(0);
      return new ElfLoader(in);
    }
    in.close();
    throw new RuntimeException("Not an ELF file");
  }
  
  /**
   * Load an elf file and all its sections.
   */
  private ElfLoader(MyDataInput file) throws IOException {
    this.file = file;
    load_sections();
  }
  
  /**
   * Load a string from a string table byte array
   */
  static String load_string(byte[] strtab, int index) {
    int len = 0;
    while (strtab[index + len] != 0)
      len++;
    return new String(strtab, index, len);
  }
  
  /**
   * Load the elf header, section table, program table, symbol table (not program
   * data)
   */
  private void load_sections() throws IOException {
    // get elf header
    ehdr = new Elf32_Ehdr(file);
    
    // load section headers
    shdrs = new Elf32_Shdr[ehdr.e_shnum];
    file.seekset(ehdr.e_shoff);
    for (int n = 0; n < ehdr.e_shnum; n++) {
      shdrs[n] = new Elf32_Shdr(file);
    }
    
    // load section header string table
    if (ehdr.e_shstrndx != 0) {
      Elf32_Shdr sec = shdrs[ehdr.e_shstrndx];
      Driver.opt().elflog("loading section header string table at %x (%d)", sec.sh_offset, sec.sh_size);
      byte[] strtab = new byte[sec.sh_size];
      file.seekset(sec.sh_offset);
      file.read(strtab, 0, strtab.length);
      for (int n = 0; n < shdrs.length; n++) {
        shdrs[n].name = load_string(strtab, shdrs[n].sh_name);
      }
    }
    
    // load RegInfo (the value of register 28)
    // it's in the mips psabi book
    for (int n = 0; n < ehdr.e_shnum; n++) {
      if (shdrs[n].sh_type == Elf32_Shdr.SHT_MIPS_REGINFO) {
        file.seekset(shdrs[n].sh_offset + 20);
        gp = file.readint();
        break;
      }
    }
    
    // load program headers
    phdrs = new Elf32_Phdr[ehdr.e_phnum];
    file.seekset(ehdr.e_phoff);
    for (int n = 0; n < ehdr.e_phnum; n++) {
      phdrs[n] = new Elf32_Phdr(file);
    }
    
    // find interp prog header
    for (int n = 0; n < ehdr.e_phnum; n++) {
      Elf32_Phdr p = phdrs[n];
      int type = p.p_type;
      if (type == Elf32_Phdr.PT_INTERP) {
        // sub 1 for terminating null
        byte[] buf = new byte[p.p_memsz - 1];
        file.seekset(p.p_offset);
        file.read(buf, 0, buf.length);
        String name = new String(buf);
        Driver.opt().elflog("ElfLoader: interpreter is " + name);
        // actually do something with interpreter...
        throw new RuntimeException("can't do dynamic executables");
      }
    }
    
    // load symbol table
    for (int n = 0; n < shdrs.length; n++) {
      // find the symbol table section
      Elf32_Shdr s = shdrs[n];
      boolean addend = false;
      
      switch (s.sh_type) {
        case Elf32_Shdr.SHT_SYMTAB:
          // load string table
          Elf32_Shdr stsec = shdrs[s.sh_link];
          Driver.opt().elflog("ElfLoader: found symbol table %s string table %s", s.name, stsec.name);
          byte[] strtab = new byte[stsec.sh_size];
          file.seekset(stsec.sh_offset);
          file.read(strtab, 0, strtab.length);
          // now load symbol table
          file.seekset(s.sh_offset);
          st = new SymbolTable(file, strtab, shdrs, n);
          break;
          
        case Elf32_Shdr.SHT_RELA:
          addend = true;
        case Elf32_Shdr.SHT_REL:
          // load relocations, these are not actually needed though
          int num = s.sh_size / s.sh_entsize;
          Driver.opt().elflog("ElfLoader: found relocations %s addend: %s num: %d", s.name, addend, num);
          rela = new Elf32_Rela[num];
          file.seekset(s.sh_offset);
          for (int r = 0; r < num; r++)
            rela[r] = new Elf32_Rela(file, addend);
          break;
      }
      
    }
    
    if (st == null) {
      // there may not be a symbol table, create an empty one
      Driver.opt().elflog("ElfLoader: no symbol table");
      st = new SymbolTable();
    }
    if (rela == null)
      rela = new Elf32_Rela[0];
  }
  
  /**
   * Load the elf file program data into a memory image.
   * TODO better handling of io exceptions, maybe return null
   */
  public Memory load (Memory mem) throws IOException {
    mem.setsymbols(st);
    
    // find the data section
    for (int n = 0; n < shdrs.length; n++) {
      Elf32_Shdr sec = shdrs[n];
      if (sec.name != null && sec.name.equals(".data")) {
        Driver.opt().elflog("data section is %s", st.getnamesafe(sec.sh_addr));
        mem.put("data", sec.sh_addr);
        break;
      }
    }
    
    FileDesc uf = file.getfile();
    
    // need an aux data vector on the stack that contains fd of the file above
    // see p.47 of mips psabi
    
    int brk = 0, bss = 0;
    
    // if e_type = shared, use mmap-style allocation rather than using vaddr
    // see linux/fs/binfmt_elf.c 377
    int loadoff = 0;
    boolean relocate = ehdr.e_type != Elf32_Ehdr.ET_EXEC;
    
    // copy from file into memory image
    // read filesz bytes to vaddr, from offset, zero up to memsz
    for (int n = 0; n < phdrs.length; n++) {
      Elf32_Phdr ph = phdrs[n];
      if (ph.p_type != Elf32_Phdr.PT_LOAD)
        continue;
      
      int loadaddr = mem.alloc(loadoff + ph.p_vaddr, ph.p_memsz);
      if (relocate && loadoff == 0) {
        Driver.opt().elflog("ElfLoader: load offset is %x\n", loadaddr);
        st.setloadoff(loadaddr);
        this.loadaddr = loadaddr;
        loadoff = loadaddr;
      }
      
      uf.seekset(ph.p_offset);
      mem.read_from(uf, loadaddr, ph.p_filesz);
      
      // elf spec says zero the difference
      if (ph.p_memsz > ph.p_filesz)
        mem.memset(loadaddr + ph.p_filesz, (byte) 0, (ph.p_memsz - ph.p_filesz));
      
      // brk is max phdr vaddr + memsz, see linux/fs/binfmt_elf.c
      brk = Math.max(brk, loadaddr + ph.p_memsz);
      bss = Math.max(bss, loadaddr + ph.p_filesz);
    }
    
    mem.setbrk(brk);
    
    //Driver.options().elflog("ElfLoader: brk=%x bss=%x\n", brk - loadaddr, bss - loadaddr);
    mem.put("brk", brk);
    mem.put("bss", bss);
    mem.put("gp", gp);
    
    return mem;
  }
  
  /**
   * Get the aux data vector, always a multiple of two in size.
   * See linux/fs/binfmt_elf.c
   */
  public int[] getaux() {
    int[] aux = new int[26];
    aux[0] = AT_HWCAP;
    aux[1] = 0;
    aux[2] = AT_PAGESZ;
    aux[3] = 65536;
    aux[4] = AT_CLKTCK;
    aux[5] = 0;
    aux[6] = AT_PHDR;
    aux[7] = ehdr.e_phoff; // plus load offset?
    aux[8] = AT_PHENT;
    aux[9] = ehdr.e_phentsize;
    aux[10] = AT_PHNUM;
    aux[11] = ehdr.e_phnum;
    aux[12] = AT_BASE;
    aux[13] = 0; // should be address of ld-linux
    aux[14] = AT_FLAGS;
    aux[15] = 0;
    aux[16] = AT_ENTRY;
    aux[17] = ehdr.e_entry; // plus load offset?
    aux[18] = AT_UID;
    aux[19] = 0;
    aux[20] = AT_EUID;
    aux[21] = 0;
    aux[22] = AT_GID;
    aux[23] = 0;
    aux[24] = AT_EGID;
    aux[25] = 0;
    return aux;
  }
  
  /**
   * Return the value fo the gp register, Glibc doesn't seem to need it though.
   */
  public int get_gp () {
    return gp;
  }
  
  /**
   * Get the entry address (initial program counter, typically _start)
   */
  public int get_entry () {
    return loadaddr + ehdr.e_entry;
  }
  
  /**
   * Close the file backing this ElfLoader
   */
  public void close() {
    file.close();
  }
  
  /**
   * Optionally print elf header, section headers, program headers and symbol table
   * as string
   */
  private String toString (boolean pr_sec, boolean pr_prog, boolean pr_sym, boolean pr_rel) {
    StringBuilder s = new StringBuilder(64 * 1024);
    s.append("Elf Header:\n").append(ehdr).append("\n");
    if (pr_prog)
      for (int n = 0; n < phdrs.length; n++)
        s.append("Prog Header ").append(n).append(":\n").append(phdrs[n]).append("\n");
    if (pr_sec)
      for (int n = 0; n < shdrs.length; n++)
        s.append("Section Header ").append(n).append(":\n").append(shdrs[n]).append("\n");
    if (pr_sym)
      s.append(st);
    if (pr_rel)
      for (int n = 0; n < rela.length; n++)
        s.append("Relocation ").append(n).append(":\n").append(rela[n]).append(" -> ").append(st.getname(rela[n].r_offset)).append("\n");
    return s.toString();
  }

  /**
   * Return a string of the ELF header
   */
  public String toString() {
    return toString(false, false, false, false);
  }
  
  private final static String usage = "ElfLoader: Prints ELF file sections. Options: -spy";
  
  /**
   * ElfLoader is also a program that can emit ELF information
   */
  public static void main(String[] args) throws Exception {
    boolean psec = false, pprog = false, psym = false, prel = false;
    String filename;
    if (args.length == 0) {
      System.err.println(usage);
      return;
    }
    
    if (args[0].charAt(0) == '-') {
      if (args.length == 1) {
        System.err.println(usage);
        return;
      }
      psec = args[0].indexOf('s') >= 0;
      pprog = args[0].indexOf('p') >= 0;
      psym = args[0].indexOf('y') >= 0;
      prel = args[0].indexOf('r') >= 0;
      filename = args[1];
    } else {
      filename = args[0];
    }
    
    Driver.opt().elflog = true;
    MyDataInput in = new MyRAFDataInput(new RandomAccessFile(filename, "r"));
    ElfLoader e = loadelf(in);
    System.out.println(e.toString(psec, pprog, psym, prel));
  }
  
} // end of class ElfLoader
