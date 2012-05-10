/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu.mips;
import static cfern.cpu.mips.MipsIsn.*;
import cfern.Driver;

/**
 * Emulates the mips coprocessor (the floating point unit).
 * Field names: op, rs=fmt, rt=ft, rd=fs, sa=fd, fn.
 * TODO consider keeping backing arrays as float
 */
final class MipsCoproc {
  
  /**
   * Index into freg of where to store registers when saving state
   */
  private static final int fccri = 32, fcsri = 33;
  
  /**
   * The fp registers. Use raw bit conversions in Float/Double for those types.
   * Last two elements are for saving/restoring state.
   */
  private final int[] freg = new int[34];
  
  /** needs machine registers to copy to and from gprs */
  private final int[] reg;
  
  /** needs machine to call branch from RS_BC (branch on coprocessor condition) */
  private final Mips machine;
  
  /** floating point condition codes register */
  private boolean fccr = false;
  
  /** floating point condition and status register */
  private int fcsr = 0;
  
  /**
   * Rounding mode as chosen by a call to setfcsr
   */
  private Round round = Round.none;
  
  /**
   * Make a new coprocessor. Needs Mips to call branch() and reg to copy values
   * between fp regs and gprs.
   */
  MipsCoproc(Mips mach, int[] reg) {
    this.machine = mach;
    this.reg = reg;
  }
  
  /**
   * copy a coprocessor as part of fork
   */
  void load(MipsCoproc other) {
    System.arraycopy(other.freg, 0, freg, 0, freg.length);
    fccr = other.fccr;
    fcsr = other.fcsr;
  }
  
  /**
   * Get the state of this class so it can be saved or loaded.
   * Must call restore after loading.
   */
  int[] getregs() {
    freg[fccri] = fccr ? 1 : 0;
    freg[fcsri] = fcsr;
    return freg;
  }
  
  /**
   * Restore the state of the float machine from the just restored registers
   */
  void restore() {
    fccr = freg[fccri] != 0;
    setfcsr(freg[fcsri]);
  }
  
  /**
   * load double from two int registers
   */
  private double load_double (int in) {
    int[] fregl = this.freg;
    long mask = 0xffffffffL;
    return Double.longBitsToDouble((fregl[in] & mask) | ((fregl[in + 1] & mask) << 32));
  }
  
  /**
   * store double into two int registers
   */
  private void store_double(int i, double d) {
    long dl = Double.doubleToRawLongBits(d);
    int[] fregl = this.freg;
    fregl[i] = (int) dl;
    fregl[i+1] = (int) (dl >>> 32);
  }
  
  /**
   * Load word register. Mips needs to be able to see this
   */
  final int load_word(int i) {
    return freg[i];
  }
  
  /**
   * Store word register
   */
  final void store_word(int i, int w) {
    freg[i] = w;
  }
  
  /** load single from int register */
  private float load_single(int i) {
    return Float.intBitsToFloat(freg[i]);
  }
  
  /** store single into int register */
  private void store_single(int i, float f) {
    freg[i] = Float.floatToRawIntBits(f);
  }
  
  /**
   * Execute the given coprocessor instruction (one with op=OP_COP1).
   */
  final void call_coproc (int isn, int rt, int rs) {
    // decode fields
    int fn = isn & 0x3f;
    int sa = (isn >>> 6) & 0x1f;
    int rd = (isn >>> 11) & 0x1f;
    //int rt = (isn >>> 16) & 0x1f;
    //int rs = (isn >>> 21) & 0x1f;
    
    switch (rs) {
    
    case RS_MF:
      // move word from floating point reg to gpr
      reg[rt] = load_word(rd);
      return;
      
    case RS_MT:
      // move word to floating point from gpr
      store_word(rd, reg[rt]);
      return;
      
    case RS_FMT_SINGLE:
      // single precision instruction. conv to double
      if (fn < CFN_CVT) {
        double res = call_fpu_fn(fn, load_single(rt), load_single(rd));
        store_single(sa, round.rf(res));
      } else {
        call_fpu_cond(fn, sa, load_single(rt), load_single(rd));
      }
      return;
      
    case RS_FMT_DOUBLE:
      // double precision instuction
      if (fn < CFN_CVT) {
        double res = call_fpu_fn(fn, load_double(rt), load_double(rd));
        store_double(sa, round.rd(res));
      } else {
        call_fpu_cond(fn, sa, load_double(rt), load_double(rd));
      }
      return;
      
    case RS_FMT_WORD:
      // word precision instruction. conv to double
      if (fn < CFN_CVT)
        store_word(sa, (int) call_fpu_fn(fn, load_word(rt), load_word(rd)));
      else
        call_fpu_cond(fn, sa, load_word(rt), load_word(rd));
      return;
      
    case RS_BC:
      /*
       * branch on fp condition 'tf'. requires access to machine.
       */
      if (((isn & 0x10000) != 0) == fccr)
        machine.branch(isn);
      return;
      
    case RS_CF:
      // move control word from floating point. 31=fcsr
      if (rd == 31)
        reg[rt] = fcsr;
      else
        throw new RuntimeException("RS_CF: attempt to read unimplemented fp control register " + rd);
      return;
      
    case RS_CT:
      // move control word to floating point. 31=fcsr
      if ((reg[rt] > 1) || (rd != 31))
        throw new RuntimeException("RS_CT: attempt to write unimplemented fp control register " + rd);
      setfcsr(reg[rt]);
      return;
      
    default:
      throw new RuntimeException(String.format("invalid coproc rs instruction: %x hex", rs));
    
    } // end of switch (rs)
  } // end of call_coproc
  
  /**
   * Set the value and associated values of the condition and status register
   */
  private void setfcsr(int fcsr) {
    this.fcsr = fcsr;
    if ((fcsr & ~0x3) != 0)
      Driver.opt().error("setfcsr: unknown mode %x\n");
    int rm = fcsr & 0x3;
    if (rm == FCSR_RM_RN)
      round = Round.none;
    else if (rm == FCSR_RM_RZ)
      round = Round.rz;
    else if (rm == FCSR_RM_RP)
      round = Round.rpi;
    else if (rm == FCSR_RM_RM)
      round = Round.rni;
  }
  
  /**
   * execute fpu function
   */
  private static double call_fpu_fn (int fn, double ft, double fs) {
    switch (fn) {
    case CFN_ADD_D: // add double
      return fs + ft;
    case CFN_SUB_D: // subtract double
      return fs - ft;
    case CFN_MUL_D: // multiply
      return fs * ft;
    case CFN_DIV_D: // divide
      return fs / ft;
    case CFN_ABS_D: // absolute
      return fs >= 0.0 ? fs : -fs;
    case CFN_MOV_D: // move
      return fs;
    case CFN_NEG_D: // negate
      return -fs;
    }
    throw new RuntimeException(String.format("invalid fpu fn: %x hex", fn));
  }
  
  /**
   * Execute fpu conversion or condition set.
   * See page 86 of MIPS Vol II pdf.
   */
  private void call_fpu_cond (int fn, int sa, double ft, double fs) {
    switch (fn) {
    case CFN_CVTS: // convert to single (20h)
      store_single(sa, (float) fs);
      return;
    case CFN_CVTD: // floating point convert to double floating point (21h)
      store_double(sa, fs);
      return;
    case CFN_CVTW: // convert to word. should round? (24h)
      store_word(sa, (int) fs);
      return;
    case CFN_FC_ULT: // unordered or less than
      fccr = Double.isNaN(fs) || Double.isNaN(ft) || fs < ft;
      return;
    case CFN_FC_EQ: // compare for equal (32h)
      fccr = (fs == ft);
      return;
    case CFN_FC_LT: // compare for less than (3ch)
      fccr = (fs < ft);
      return;
    case CFN_FC_LE: // less then or equal (3eh)
      fccr = fs <= ft;
      return;
    }
    
    throw new RuntimeException(String.format("invalid fpu cond: %x hex", fn));
  }
  
} // end of class MipsFloat
