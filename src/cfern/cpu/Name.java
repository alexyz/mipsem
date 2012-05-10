/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;

/**
 * A dissasmbly name and type. Immutable. Initalised in MipsIsn though it should
 * be generic.
 */
public class Name {

  public final String name;
  public final char type;
  //public final byte arity;

  /**
   * Instruction name and disasm type (hard coded)
   */
  public Name(String s, char c) {
    name = s;
    type = c;
    //arity = 0;
  }

  /**
   * Instruction name, assumes type R
   */
  public Name(String s) {
    name = s;
    type = 'R';
    //arity = 0;
  }
  
  /**
   * Syscall, number of arguments
   */
  public Name(String s, byte a) {
    // no type provided, assume R
    name = s;
    type = 'R';
    //arity = a;
  }

} // end of class Name
