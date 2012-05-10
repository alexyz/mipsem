/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu.mips;

/**
 * Abstract class for rounding modes.
 * This seems to add about 5% to cpu load compared with no rounding whatsoever.
 */
abstract class Round {
  
  /**
   * Round towards zero (truncate)
   */
  static final Round rz = new Round() {
    final double rd(double d) {
      return d > 0.0 ? StrictMath.floor(d) : StrictMath.ceil(d);
    }
    final float rf(double d) {
      return (float) (d > 0.0 ? StrictMath.floor(d) : StrictMath.ceil(d));
    }
  };
  
  /**
   * Round towards positive infinity
   */
  static final Round rpi = new Round() {
    final double rd(double d) {
      return StrictMath.ceil(d);
    }
    final float rf(double d) {
      return (float) StrictMath.ceil(d);
    }
  };
  
  /**
   * Round towards negative infinity
   */
  static final Round rni = new Round() {
    final double rd(double d) {
      return StrictMath.floor(d);
    }
    final float rf(double d) {
      return (float) StrictMath.floor(d);
    }
  };
  
  /**
   * No rounding
   */
  static final Round none = new Round() {
    final double rd(double d) {
      return d;
    }
    final float rf(double d) {
      return (float) d;
    }
  };
  
  /**
   * Round this double to a double
   */
  abstract double rd(double d);
  
  /**
   * Round this double to a float
   */
  abstract float rf(double f);
  
} // end of class Round
