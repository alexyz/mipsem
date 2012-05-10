/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu;

/**
 * Thrown at end of program. Caught by run() which then returns. Should be
 * subclass of exception really but this is a bit easier to work with
 */

public final class EndOfProgramException extends RuntimeException {
  //
}
