/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu.mips;
import static cfern.cpu.mips.MipsIsn.*;

import java.io.IOException;
import java.util.*;
import cfern.*;
import cfern.cpu.*;
import cfern.elf.*;
import cfern.mem.Memory;
import cfern.sys.str.SigAction;
import cfern.sys.Constants;
import cfern.sys.SignalHandler;

/**
 * A MIPS R2000 emulator, now with less state!
 * Call start() to begin.
 * TODO make funlog take account of sp
 */
public final class Mips extends Machine {

	/**
	 * Index into reg array for these special registers
	 */
	private final static int reghi = 32, reglo = 33, pci = 34, nextpci = 35, maski = 36;

	/**
	 * General purpose registers. 0=zero 1=at 2=v0 3=v1 4=a0 5=a1 6=a2 7=a3 8=t0
	 * 9=t1 10=t2 11=t3 12=t4 13=t5 14=t6 15=t7 16=s0 17=s1 18=s2 19=s3 20=s4
	 * 21=s5 22=s6 23=s7 24=t8 25=t9 26=k0 27=k1 28=gp 29=sp 30=s8 31=ra
	 * 32=reghi 33=reglo 34=pc 35=nextpc 36=mask 37=unused
	 */
	private final int[] reg = new int[38];

	/**
	 * program counter and next program counter. pc is the address of the next
	 * (not current) instruction. nextpc is always pc+4, unless a branch
	 * instruction changes it. (this is to emulate the mips branch delay slot).
	 * thus there is no way to know the address of the current instruction, though
	 * pc-4 is most likely.
	 */
	private int pc, nextpc;

	/** floating point emulator */
	private final MipsCoproc coproc;

	/**
	 * Signal return address
	 */
	private int sigreturn_p = 0;

	/**
	 * Create a mips emulator thread. Load elf file with load(). This is only
	 * called once, for the initial machine. Everything else is forked.
	 */
	public Mips() {
		super();
		coproc = new MipsCoproc(this, reg);
	}

	/**
	 * Create a machine by COPYING the other machine as part of fork().
	 * Copies sys too, but gets new pid.
	 */
	private Mips(Mips other) {
		super(other);
		coproc = new MipsCoproc(this, reg);

		// copy other machine
		fun.load(other.fun);
		coproc.load(other.coproc);
		System.arraycopy(other.reg, 0, reg, 0, reg.length);
		pc = other.pc;
		nextpc = other.nextpc;
		sigreturn_p = other.sigreturn_p;

		// make fork return 0 with error 0
		reg[2] = 0;
		reg[7] = 0;

		setName(other.getName() + "." + getpid());
	}

	public Mips copy() {
		return new Mips(this);
	}

	/**
	 * Load an elf file into this machine. Used before starting or from execve.
	 * Does NOT modify system interface.
	 */
	public void load (ElfLoader elf, List<String> args, List<String> env) {
		if (isAlive() && Thread.currentThread() != this)
			throw new RuntimeException("can only load before start or from execve");

		// clear and load memory
		// TODO read stack size from env
		mem.clear();
		mem.allocstack(4 * 65536);
		fun.clear();
		try {
			elf.load(mem);
			elf.close();
		} catch (IOException e) {
			throw new RuntimeException("elf load err", e);
		}

		// find breakpoints in env
		for (int n = 0; n < env.size(); n++) {
			String var = env.get(n);
			if (var.startsWith("bp=")) {
				fun.setbreakpoints(var.substring(3));
				break;
			}
		}

		// above code is generic (except fun...), should be in Machine.load

		sigreturn_p = mem.push_words(MipsIsn.sigreturn);
		mem.store_args_and_env(args, env);

		// load regs
		Arrays.fill(reg, 0);
		pc = elf.get_entry();
		nextpc = pc + 4;
		reg[29] = mem.getsp();

		// set name of thread
		String prog = args.get(0);
		int sl = prog.lastIndexOf("/");
		String name = ((sl >= 0) ? prog.substring(sl + 1) : prog) + "." + getpid();
		setName(name);
		if (isAlive())
			Machines.update(this);

		opt.debug("# loaded %d args, %d envs, sp=%s pc=%s", 
				args.size(), env.size(), mem.getname(reg[29]), mem.getname(pc));
	}

	/**
	 * Prepare to invoke a function in the target.
	 * Saves state.
	 */
	protected void invoke(SigAction act) {
		opt.siglog("invoke: %s", act.toString(mem));

		// set mask
		SignalHandler sig = sys.signal().getSignalHandler();
		reg[maski] = sig.getmask();
		sig.setmask(act.getmask());

		// push int regs onto user stack
		int sp = reg[29];
		reg[pci] = pc;
		reg[nextpci] = nextpc;
		sp = mem.push_words(sp, reg);

		// push float regs
		sp = mem.push_words(sp, coproc.getregs());

		// set signal state
		Arrays.fill(reg, 0);
		pc = act.handler();
		nextpc = pc + 4;
		reg[4] = act.getnum();
		reg[25] = pc;
		// gcc likes to overwrite [sp] so nudge it down a bit
		reg[29] = sp - 8;
		// return to a trampoline that invokes restore() via syscall
		reg[31] = sigreturn_p;

		if (opt.slow)
			fun.call(pc, reg[29]);
	}

	/**
	 * Restore saved signal state and allow interrupts. Called from syscall
	 * handler. Note: this may never be called, as it is permissable for a signal
	 * handler to exit using longjmp.
	 */
	protected void restore() {
		int sp = reg[29] + 8;
		sp = mem.load_words(sp, coproc.getregs());
		sp = mem.load_words(sp, reg);
		if (reg[29] != sp)
			throw new RuntimeException("unbalanced restore");
		coproc.restore();
		pc = reg[pci];
		nextpc = reg[nextpci];
		sys.signal().getSignalHandler().setmask(reg[maski]);
	}

	/**
	 * Begin program execution. Calls the instruction decoder runfast() or
	 * rundebug(). Does not throw runtime exception. Return -1 for bad exit, 0-255
	 * for good exit.
	 */
	public byte runImpl () {
		byte exitval = 0;
		try {
			// call instruction scheduler.
			if (opt.slow)
				rundebug();
			else
				runfast();
		} catch (EndOfProgramException e) {
			exitval = (byte) reg[4];
		} catch (RuntimeException e) {
			e.printStackTrace();
			// print faulty instruction, bad memory ref or undefined syscall
			opt.println(regs());
			opt.println(disasm(mem, reg, pc - 4, pc));
			opt.println("fun bt: %s", fun.bt());
			opt.println("stack bt: %s", backtrace());
			exitval = -1;
		}
		return exitval;
	}

	/**
	 * Run a program with debugging options (that may slow down execution)
	 */
	private void rundebug() {
		int isn, op;
		boolean bp = false, call = false, ret = false;

		for (;;) {
			// check for a signal after every instruction? not worth it
			//service();

			isn = mem.load_word(pc);
			op = isn >>> 26;

			if (opt.disasm || bp) {
				if (opt.regprint)
					opt.println(regs());
				opt.println(disasm(mem, reg, pc, nextpc));
				if (opt.interactive)
					Driver.step();
			}

			pc = nextpc;
			nextpc = nextpc + 4;
			if (op != 0)
				call_op(isn);
			else
				call_fn(isn);

			// just to humour myself
			// note that $26 and $27 are also always 0
			if (reg[0] != 0)
				throw new RuntimeException("reg 0 failure");

			// perhaps should not trace calls in signal
			if (call) {
				bp = fun.call(pc, reg[29]);
				if (opt.funlog)
					opt.println("%s  (%x,%x,%x,%x)\n", fun.bt(), reg[4], reg[5], reg[6], reg[7]);
				call = false;
			} else if (ret) {
				if (opt.funlog)
					opt.println("# %s returns %x (%d)\n", fun.top(), reg[2], reg[2]);
				bp = fun.ret(reg[29]);
				ret = false;
			}

			// do a funcall trace, but calls/rets don't actually happen until the next instruction
			if (op == OP_JAL) {
				call = true;
			} else if (op == 0) {
				int fn = isn & 0x3f;
				if (fn == FN_JALR)
					call = true;
				else if (fn == FN_JR && ((isn >>> 21) & 0x1f) == 31) // rs
				ret = true;
			}
		} // infinite loop
	}

	/**
	 * Run a program as fast as possible. Signals are not checked until a syscall
	 */
	private void runfast() {
		Memory mem = super.mem;
		for (;;) {
			int isn = mem.load_word(pc);
			// faster to do this even though there's two getfields
			pc = nextpc;
			nextpc += 4;
			if (isn >>> 26 != 0)
				call_op(isn);
			// faster to check for nop here than above call_op
			else if (isn != 0)
				call_fn(isn);
		}
	}

	/**
	 * calculate nextpc from (26 bit) instr_index to (28 bit) target within 256mb region
	 */
	private void target(int isn) {
		nextpc = (pc & 0xf0000000) | ((isn & 0x3FFFFFF) << 2);
	}

	/**
	 * Get a stack backtrace
	 */
	private String backtrace() {
		// there are 3 cases:
		// 1. in a leaf function
		// 2. in non leaf function (either before or after a branch)
		// 3. jump to a null pointer (always a crash)
		StringBuilder sb = new StringBuilder(256);

		// first check pc (may be null after a bad jump)
		String f = mem.getfunname(pc);
		int fa = mem.getaddress(f);
		int sp = reg[29];

		// now get return address for 3 cases
		int a;
		if (!mem.bound(pc)) {
			sb.append(" [crash after jump to null]");
			a = reg[31];
		} else {
			int raoff = findraslot(mem, fa);
			if (raoff > 0) {
				a = mem.load_word(sp + raoff);
				sb.append(" [non leaf function " + (reg[31] == a ? "before" : "after") + " branch]");
			} else {
				a = reg[31];
				sb.append(" [leaf function]");
			}
			int fs = findframesize(mem, fa);
			// non leaf will have frame, leaf may or may not
			if (fs > 0)
				sp = sp + fs;
		}

		sb.insert(0, mem.getname(pc));

		while (mem.bound(a)) {
			//sb.append("/").append(mem.getname(a));
			sb.insert(0, ' ').insert(0, mem.getname(a));
			f = mem.getfunname(a);
			fa = mem.getaddress(f);
			if (fa == 0) {
				sb.append(" [could not get address of function " + f + "]");
				break;
			}
			int fs = findframesize(mem, fa);
			if (fs < 0) {
				if (!f.equals("hlt"))
					sb.append(" [could not get frame size of " + f + "]");
				break;
			}
			int raoff = findraslot(mem, fa);
			if (raoff < 0) {
				// not likely if there is a frame
				sb.append(" [could not get return slot of " + f + "]");
				break;
			}
			a = mem.load_word(sp + raoff);
			sp = sp + fs;
		}

		return sb.toString();
	}

	/**
	 * Calculates the new pc from a signed 16 bit immediate offset, ie a pc
	 * relative branch. Only to be used by the float machine (call_op has its own
	 * version)
	 */
	void branch (int isn) {
		short simm = (short) isn;
		nextpc = pc + (simm * 4);
	}

	/**
	 * Call a normal operation instruction. Only needs fields rt,rs,simm.
	 * Instructions should RETURN unless they need a relative branch in which case
	 * they should BREAK. A bit ugly but faster.
	 */
	private void call_op (int isn) {
		int rt = (isn >>> 16) & 0x1f;
		int rs = (isn >>> 21) & 0x1f;
		int[] reg = this.reg; // hack to avoid loads of getfields
		short simm = (short) isn;

		// slightly faster to re-evaluate op than pass it as a parameter
		switch (isn >>> 26) {
			case OP_SPECIAL:
				// not actually used
				call_fn(isn);
				return;
			case OP_REGIMM:
				// a register-immediate instruction (all branches)
				switch (rt) {
					case RT_BGEZAL: // branch on greater than or equal to zero and link
						reg[31] = nextpc; 
						// fall through
					case RT_BGEZ: // branch if greater than or equal to zero
						if (reg[rs] >= 0)
							break;
						return;
					case RT_BLTZAL: // branch on less than zero and link
						// NOTE: by this, they mean link, then conditionally branch
						reg[31] = nextpc;
						// fall through
					case RT_BLTZ: // branch on less than zero
						if (reg[rs] < 0)
							break;
						return;
					default:
						throw new RuntimeException("invalid rt: " + rt);
				}
				break;
			case OP_COP1: // call coprocessor
				coproc.call_coproc(isn, rt, rs);
				return;
			case OP_LWC1: // load word from mem to coprocessor
				coproc.store_word(rt, mem.load_word(reg[rs] + simm));
				return;
			case OP_SWC1: // store word from coprocessor to memory
				mem.store_word(reg[rs] + simm, coproc.load_word(rt));
				return;
			case OP_J: // branch within 256mb region
				target(isn);
				return;
			case OP_JAL: // jump and link
				reg[31] = nextpc; // thispc + 8 ?
				target(isn);
				return;
			case OP_BLEZ: // branch on less than or equal to zero
				if (reg[rs] <= 0)
					break;
				return;
			case OP_BEQ: // branch on equal
				if (reg[rs] == reg[rt])
					break;
				return;
			case OP_BNE: // branch on not equal
				if (reg[rs] != reg[rt])
					break;
				return;
			case OP_ADDIU: // add immediate unsigned word
				reg[rt] = reg[rs] + simm;
				return;
			case OP_ANDI: // and immediate (zero extend)
				reg[rt] = reg[rs] & (simm & 0xffff);
				return;
			case OP_XORI: // exclusive or immediate (zx)
				reg[rt] = reg[rs] ^ (simm & 0xffff);
				return;
			case OP_BGTZ: // branch on greater than zero
				if (reg[rs] > 0) break;
				return;
			case OP_SLTI: // set on less than immediate. compare both as signed.
				reg[rt] = reg[rs] < simm ? 1 : 0;
				return;
			case OP_SLTIU: // set on less than immediate unsigned. sign extend imm and compare as unsigned.
				reg[rt] = ((reg[rs] & 0xffffffffL) < (simm & 0xffffffffL)) ? 1 : 0;
				return;
			case OP_ORI: // or immediate. zx
				reg[rt] = reg[rs] | (simm & 0xffff);
				return;
			case OP_SW: // store word.
				mem.store_word(reg[rs] + simm, reg[rt]);
				return;
			case OP_SH: // store halfword
				mem.store_half(reg[rs] + simm, ((short) (reg[rt] & 0xffff)));
				return;
			case OP_SB: // store byte
				mem.store_byte(reg[rs] + simm, ((byte) (reg[rt] & 0x000000ff)));
				return;
			case OP_LUI: // load upper immediate (la in ncc)
				reg[rt] = /*((int)simm)*/ simm << 16;
				return;
			case OP_LW: // load word. will suck for addresses over 2gb ? no, shouldn't matter
				reg[rt] = mem.load_word(reg[rs] + simm);
				return;
			case OP_LB: // load byte (signed)
				reg[rt] = mem.load_byte(reg[rs] + simm);
				return;
			case OP_LBU: // load unsigned byte
				reg[rt] = mem.load_byte(reg[rs] + simm) & 0x000000ff;
				return;
			case OP_LHU: // load halfword unsigned
				reg[rt] = (mem.load_half(reg[rs] + simm) & 0xffff);
				return;
			case OP_LH: // load halfword. sign extend
				reg[rt] = mem.load_half(reg[rs] + simm);
				return;
			case OP_LWL: { // load word left. the horror!
				int a = reg[rs] + simm;
				int s = (a & 3) << 3;
				reg[rt] = (mem.load_word(a & ~3) << s) | (reg[rt] & ((int) (0xffffffffL >>> (32 - s))));
				return;
			} 
			case OP_LWR: { // load word right. least signicant byte at eff.addr.
				int a = reg[rs] + simm;
				int s = ((a & 3) + 1) << 3;
				reg[rt] = (mem.load_word(a & ~3) >>> (32 - s)) | (reg[rt] & ((int) (0xffffffffL << s)));
				return;
			} 
			case OP_SWL: { // store word left
				int a = reg[rs] + simm; // msb
				int b = a & ~3; // aligned address
				int s = (a & 3) << 3;
				mem.store_word(b, (reg[rt] >>> s) | (mem.load_word(b) & ((int) (0xffffffffL << (32 - s)))));
				return;
			}
			case OP_SWR: { // store word right
				int a = reg[rs] + simm; // lsb
				int b = a & ~3;
				int s = ((a & 3) + 1) << 3;
				mem.store_word(b, (reg[rt] << (32 - s)) | (mem.load_word(b) & ((int) (0xffffffffL >>> s))));
				return;
			}

			default: // instruction not implemented
				throw new RuntimeException(String.format("invalid op %02x", isn >> 26));
		}

		// do a relative branch. basically a manually inlined branch()
		nextpc = pc + (simm * 4); // thispc + (simm << 2) + 4 ?
	} // end of call_op()

	/**
	 * a register instruction. uses fn,rd,rt,rs and sa (not often, manaully decode)
	 */
	private void call_fn (int isn) {
		int[] reg = this.reg; // hack to avoid getfield
		int rd = (isn >>> 11) & 0x1f;
		int rt = (isn >>> 16) & 0x1f;
		int rs = (isn >>> 21) & 0x1f;
		int fn = isn & 0x3f;

		switch (fn) {
			case FN_SYSCALL: // syscall
				syscall(isn);
				// after a syscall check there are no outstanding signals
				// can't do it generically because we can't change the machine state during a syscall
				service();
				return;
			case FN_SLL: // shift word left logical. also nop if sa,rd,rt = 0
				reg[rd] = reg[rt] << ((isn >>> 6) & 0x1f); 
				return;
			case FN_SRL: // shift word right logical
				reg[rd] = reg[rt] >>> ((isn >>> 6) & 0x1f); 
				return;
			case FN_SRA: // shift word right arithmetic (preserve sign)
				reg[rd] = reg[rt] >> ((isn >>> 6) & 0x1f); 
				return;
			case FN_SRLV: // shift word right logical variable (shift amount in reg[rs])
				reg[rd] = reg[rt] >>> (reg[rs] & 0x1f); 
				return;
			case FN_SRAV: // shift word right arithmetic variable (shift amount in reg[rs], propogate sign).
				reg[rd] = reg[rt] >> (reg[rs] & 0x1f); 
				return;
			case FN_SLLV: // shift word left logical variable (low order 5 bits of RS)
				reg[rd] = reg[rt] << (reg[rs] & 0x1f); 
				return;
			case FN_JR: // jump register (function call return if rs=31)
				nextpc = reg[rs]; 
				return;
			case FN_JALR: // jump and link register
				reg[rd] = nextpc;
				nextpc = reg[rs]; 
				return;
			case FN_MFHI: // move from hi register
				reg[rd] = reg[reghi]; 
				return;
			case FN_MFLO: // move from lo register
				reg[rd] = reg[reglo];
				return;
			case FN_MULT: { // multiply 32 bit signed integers. 64 bit result.
				long res = ((long) reg[rs]) * ((long) reg[rt]);
				reg[reglo] = ((int) (res & 0xffffffffL));
				reg[reghi] = ((int) ((res >>> 32) & 0xffffffffL));
				return;
			} 
			case FN_MULTU: { // mul unsigned integers
				long res = (reg[rs] & 0xffffffffL) * (reg[rt] & 0xffffffffL);
				reg[reglo] = (int) (res & 0xffffffffL);
				reg[reghi] = (int) ((res >>> 32) & 0xffffffffL);
				return;
			} 
			case FN_DIV: // divide 32 bit signed integers 
				// result is unpredictable for zero, no exceptions thrown
				if (rt != 0) {
					reg[reglo] = reg[rs] / reg[rt];
					reg[reghi] = reg[rs] % reg[rt];
				}
				return;
			case FN_DIVU: { // divide unsigned word
				// unpredictable result and no exception for zero
				if (reg[rt] != 0) {
					// use long so a and b are unsigned
					long a = (reg[rs] & 0xffffffffL), b = (reg[rt] & 0xffffffffL);
					reg[reglo] = (int) (a / b); // shouldnt be signed
					reg[reghi] = (int) (a % b);
				}
				return;
			}
			case FN_ADDU: // add unsigned word
				reg[rd] = reg[rs] + reg[rt]; 
				return;
			case FN_SUBU: // subtract unsigned word
				reg[rd] = reg[rs] - reg[rt]; 
				return;
			case FN_AND: // bitwise logical and
				reg[rd] = reg[rs] & reg[rt]; 
				return;
			case FN_OR: // bitwise logical or
				reg[rd] = reg[rs] | reg[rt]; 
				return;
			case FN_XOR: // exclusive or
				reg[rd] = reg[rs] ^ reg[rt]; 
				return;
			case FN_NOR: // not or
				reg[rd] = ~ (reg[rs] | reg[rt]); 
				return;
			case FN_SLT: // set on less than (signed !!)
				reg[rd] = (reg[rs] < reg[rt]) ? 1 : 0; 
				return;
			case FN_SLTU: // set on less than unsigned TODO create a word mask constant
				reg[rd] = ((reg[rs] & 0xffffffffL) < (reg[rt] & 0xffffffffL)) ? 1 : 0; 
				return;
		}

		throw new RuntimeException("invalid fn 0x" + Integer.toHexString(fn));
	}

	/**
	 * system call handler. connects syscalls to implementation in SystemInterface
	 */
	private void syscall (int isn) {
		// 5th arg is $29[16] 6th arg is $29[20] 7th at $29[24] (see mmap)
		int a = reg[4], b = reg[5], c = reg[6], d = reg[7], e = 0, f = 0;

		// linux syscalls start at 4000... see linux/include/asm-mips/unistd.h
		int call = reg[2] - 4000;

		if (opt.trace) {
			if (opt.regprint && !opt.disasm)
				opt.println(regs());
			//String as = a > 0x400000 ? mem.getname(a) : Integer.toString(a);
			//String bs = b > 0x400000 ? mem.getname(b) : Integer.toString(b);
			//String cs = c > 0x400000 ? mem.getname(c) : Integer.toString(c);
			//String ds = c > 0x400000 ? mem.getname(d) : Integer.toString(d);
			//System.err.println(disasm(mem, reg, pc - 4, nextpc));
			opt.println(backtrace());
			//opt.println("syscall %d %s(%s, %s, %s, %s, ...)", call, Syscalls.sys_names[call].name, as, bs, cs, ds);
			if (opt.interactive && !opt.disasm) {
				Driver.step();
			}
		}

		// minor hack until we have number of args in the disasm type
		// arg positions are fixed on stack
		// see syscall6 somewhere in glibc
		switch (call) {
			case Syscalls.SYS_MMAP:
				f = mem.load_word(reg[29] + 20);
			case Syscalls.SYS_LLSEEK:
			case Syscalls.SYS_SETSOCKOPT:
				e = mem.load_word(reg[29] + 16);
		}

		// actually do syscall
		int res = syscall(call, a, b, c, d, e, f);

		if (res < 0 && opt.warn) {
			opt.warn("%s returns %s in %s", Syscalls.sys_name(call).name, Constants.get().name("E", -res), backtrace());
		}

		// set the return value of the syscall
		// glibc expects a3 to be set to error
		if (call == Syscalls.SYS_SIGRETURN) {
			// do nothing
		} else if (call == Syscalls.SYS_PIPE && res == 0) {
			// for some weird reason linux returns pipe() result in registers
			// see linux/arch/mips/kernel/syscall.c
			reg[2] = mem.load_word(a);
			reg[3] = mem.load_word(a + 4);
			reg[7] = 0;
		} else if (res < 0) {
			reg[2] = -res; // i.e. positive error
			reg[7] = -1;
		} else {
			reg[2] = res;
			reg[7] = 0;
		}
	}

	/**
	 * Print integer registers
	 */
	private String regs() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("REGS: pc=").append(mem.getname(pc));
		for (int n = 0; n < reg.length; n++) {
			if (reg[n] == 0)
				continue;
			sb.append(' ').append(reg_names[n]);
			sb.append('=').append(mem.getname(reg[n]));
		}
		return sb.toString();
	}

} // end of class Machine
