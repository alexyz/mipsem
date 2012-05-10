/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.elf;
import java.io.*;

import cfern.fs.MyDataInput;

/**
 * ELF relocation type, TIS ELF Spec 1.2 page 36
 */
final class Elf32_Rela {
  
  final int r_offset;
  final int r_info;
  final int r_addend;
  
  public Elf32_Rela(MyDataInput in, boolean addend) throws IOException {
    r_offset = in.readint();
    r_info = in.readint();
    r_addend = addend ? in.readint() : 0;
  }
  
  public String toString() {
    return String.format("|  off: %-8x  info: %-8x  addend: %d", r_offset, r_info, r_addend);
  }
  
}
