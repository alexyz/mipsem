/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern;
import javax.sound.sampled.*;
import java.io.*;

/**
 * A class that can write data to the sound card.
 */
public final class AudioOutputStream extends OutputStream {
  
  private final byte[] buf = new byte[16384];

  /**
   * Buffer position for writing individual bytes
   */
  private int p = 0;

  private final SourceDataLine sdl;

  /**
   * Creates and starts an audio output stream, with specified samplerate, bits
   * per sample, channels, byte order (intel=bigend) and gain (0=loud,
   * -40=quiet).
   * See also AudioOutputStream.newfor.
   */
  public AudioOutputStream(int sr, int bits, int chans, boolean bigend, float gain) throws IOException {
    AudioFormat af = new AudioFormat(sr, bits, chans, true, bigend);
    DataLine.Info dli = new DataLine.Info(SourceDataLine.class, af);
    // should probably check if format is supported
    System.err.println("AudioOut: " + dli);

    try {
      sdl = (SourceDataLine) AudioSystem.getLine(dli);
      sdl.open(af, sr * 4);
    } catch (LineUnavailableException e) {
      e.printStackTrace();
      throw new IOException("Could not open audio");
    }

    try {
      // volume doesn't seem to be supported
      // also needs to be open before gain is applied
      FloatControl fc = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
      fc.setValue(gain);
      System.err.println("AudioOut: " + fc);
    } catch (Exception e) {
      // not fatal
      System.err.println("AudioOut: " + e);
    }

    sdl.start();
  }

  public void write(byte[] buf, int off, int len) throws IOException {
    if (p > 0) {
      write(buf, 0, p);
      p = 0;
    }
    //System.err.printf("AudioOut: write(byte[%d], %d, %d)\n", buf.length, off, len);
    if ((len & 3) != 0)
      System.err.printf("AudioOut: warning: non integral write (%d)\n", len);
    sdl.write(buf, off, len & ~3);
  }

  public void write(int b) throws IOException {
    buf[p++] = (byte) b;
    if (p == buf.length) {
      p = 0;
      write(buf);
    }
  }

  public void close() {
    System.err.printf("AudioOut: stopped");
    sdl.stop();
  }
  
  /**
   * Parse the arguments and return a new AudioOutputStream
   */
  public static OutputStream newInstance (String args) throws IOException {
    return newfor(args.split("\\s+"));
  }
  
  /**
   * Parse the arguments and return a new AudioOutputStream
   */
  public static OutputStream newfor (String[] args) throws IOException {
    int rate = 11025, chans = 1, bits = 16;
    float gain = -35F;
    boolean bigend = true;

    for (int n = 0; n < args.length; n++) {
      String a = args[n].toLowerCase();
      if (a.matches("1|1ch|1channel|m|mono"))
        chans = 1;
      else if (a.matches("2|2ch|2channel|st|stereo"))
        chans = 2;
      else if (a.matches("le|lit|little|littleend|littleendian|mips"))
        bigend = false;
      else if (a.matches("be|big|bigend|bigendian|intel"))
        bigend = true;
      else if (a.matches("8|8b|8bit"))
        bits = 8;
      else if (a.matches("16|16b|16bit"))
        bits = 16;
      else if (a.matches("-\\d+(db)?"))
        gain = Float.parseFloat(a.indexOf("db") == -1 ? a : a.substring(0, a.length() - 2));
      else if (a.matches("\\d+(hz)?"))
        rate = Integer.parseInt(a.replaceAll("hz", ""));
      else if (a.matches("\\d+(\\.\\d+)?khz"))
        rate = (int) (Double.parseDouble(a.substring(0, a.length() - 3)) * 1000);
      else
        System.err.printf("AudioOut: unknown arg %s\n", a);
    }
    
    return new AudioOutputStream(rate, bits, chans, bigend, gain);
  }

  /**
   * Read from system.in. Parse arguments for settings.
   */
  public static void main(String[] args) throws Exception {
    OutputStream a = newfor(args);
    byte[] buf = new byte[16384];
    int r;
    while ((r = System.in.read(buf)) > 0)
      a.write(buf, 0, r);
    /*
    int b;
    while ((b = System.in.read()) >= 0)
      a.write(b);
      */
  }

} // end of class AudioOutputStream
