/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.cpu.mips;
import java.util.Arrays;
import cfern.cpu.*;
import cfern.mem.Memory;

/**
 * Symbolic constants and names for MIPS instructions and system calls, plus
 * other utilities including a disassembler and a stack backtracer. Intended for
 * static import.
 * TODO backtrace could print argument slots
 */
public final class MipsIsn {

	static {
		op_names = new Name[64];
		fn_names = new Name[64];
		cfn_names = new Name[64];
		rs_names = new Name[32];
		rt_names = new Name[32];
		reg_nums = new String[32];
		init();
	}

	private MipsIsn() {
		// private to prevent javadoc
	}

	/**
	 * The signal restore code (addiu $2, $0, 4119; nop; syscall; nop;).
	 */
	public static final int[] sigreturn = new int[] { 0x24021017, 0, 0xc, 0 }; 

	/** integer operations, typically with 16 bit immediate */
	public static final byte
	OP_SPECIAL = 0x00,
	OP_REGIMM = 0x01,
	OP_J = 0x02,
	OP_JAL = 0x03,
	OP_BEQ = 0x04,
	OP_BNE = 0x05,
	OP_BLEZ = 0x06,
	OP_BGTZ = 0x07,
	OP_ADDIU = 0x09,
	OP_SLTI = 0x0a,
	OP_SLTIU = 0x0b,
	OP_ANDI = 0x0c,
	OP_ORI = 0x0d,
	OP_XORI = 0x0e,
	OP_LUI = 0x0f,
	OP_COP1 = 0x11,
	OP_SPEC2 = 0x1c,
	OP_LB = 0x20,
	OP_LH = 0x21,
	OP_LWL = 0x22,
	OP_LW = 0x23,
	OP_LBU = 0x24,
	OP_LHU = 0x25,
	OP_LWR = 0x26,
	OP_SB = 0x28,
	OP_SH = 0x29,
	OP_SWL = 0x2a,
	OP_SW = 0x2b,
	OP_SWR = 0x2e,
	OP_LWC1 = 0x31,
	OP_SWC1 = 0x39;

	/** integer register functions */
	public static final byte
	FN_SLL = 0x00,
	FN_SRL = 0x02,
	FN_SRA = 0x03,
	FN_SLLV = 0x04,
	FN_SRLV = 0x06,
	FN_SRAV = 0x07,
	FN_JR = 0x08,
	FN_JALR = 0x09,
	FN_MOVN = 0x0b,
	FN_SYSCALL = 0x0c,
	FN_BREAK = 0x0d,
	FN_MFHI = 0x10,
	FN_MFLO = 0x12,
	FN_MULT = 0x18,
	FN_MULTU = 0x19,
	FN_DIV = 0x1a,
	FN_DIVU = 0x1b,
	FN_ADDU = 0x21,
	FN_SUBU = 0x23,
	FN_AND = 0x24,
	FN_OR = 0x25,
	FN_XOR = 0x26,
	FN_NOR = 0x27,
	FN_SLT = 0x2a,
	FN_SLTU = 0x2b;

	/** register immediate branches */
	public static final byte
	RT_BLTZ = 0x00,
	RT_BGEZ = 0x01,
	RT_BLTZAL = 0x10,
	RT_BGEZAL = 0x11;

	/** coprocessor instructions */
	public static final byte
	RS_MF = 0x00,
	RS_CF = 0x02,
	RS_MT = 0x04,
	RS_CT = 0x06,
	RS_BC = 0x08,
	RS_FMT_SINGLE = 0x10,
	RS_FMT_DOUBLE = 0x11,
	RS_FMT_WORD = 0x14;

	/**
	 * Coprocessor format functions (fn field)
	 */
	public static final byte
	CFN_ADD_D = 0x00,
	CFN_SUB_D = 0x01,
	CFN_MUL_D = 0x02,
	CFN_DIV_D = 0x03,
	CFN_ABS_D = 0x05,
	CFN_MOV_D = 0x06,
	CFN_NEG_D = 0x07,
	CFN_CVT = 0x20, // not an instruction
	CFN_CVTS = 0x20,
	CFN_CVTD = 0x21,
	CFN_CVTW = 0x24,
	CFN_FC = 0x30, // not an instruction
	CFN_FC_EQ = 0x32,
	CFN_FC_ULT = 0x35,
	CFN_FC_LT = 0x3c,
	CFN_FC_LE = 0x3e;

	/** coproc round to nearest */
	public static final int FCSR_RM_RN = 0x0;
	/** coproc round towards zero */
	public static final int FCSR_RM_RZ = 0x1;
	/** coproc round towards plus infinity */
	public static final int FCSR_RM_RP = 0x2;
	/** coproc round towards minus infinity */
	public static final int FCSR_RM_RM = 0x3;

	/**
	 * names of all instructions and syscalls
	 */
	public static final Name[] op_names, fn_names, cfn_names, rs_names, rt_names;

	/**
	 * gas names of all registers (see also doc for Machine.regs)
	 */
	public static final String[] reg_names = new String[] {
		"zero", "at", "v0", "v1", // 0 - 3
		"a0", "a1", "a2", "a3", // 4 - 7
		"t0", "t1", "t2", "t3", // 8 - 11
		"t4", "t5", "t6", "t7", // 12 - 15
		"s0", "s1", "s2", "s3", // 16 - 19
		"s4", "s5", "s6", "s7", // 20 - 23
		"t8", "t9", "k0", "k1", // 24 - 27
		"gp", "sp", "s8", "ra", // 28 - 31
		"hi", "lo", "pc", "nextpc"};

	public static final String[] reg_nums;

	/* J jump
   I imm
   R reg (default if not specified)
   r relative branch
   b binary immediate
   B binary register
   l load
   s store
   L lwc1
   S swc1
   3 three operand fpu
   2 two operand fpu
   4 - cfc1

   automatically assumed in dissasmbler:
   v relative branch one operand 
   y syscall
   c fpu compare
	 */

	/**
	 * set up the opcode names/types array. must call from main.
	 */
	private static void init () {
		// make sure they all have non-null entries
		Name undef = new Name("undef", 'u');
		Arrays.fill(op_names, undef);
		Arrays.fill(fn_names, undef);
		Arrays.fill(cfn_names, undef);
		Arrays.fill(rs_names, undef);
		Arrays.fill(rt_names, undef);

		for (int n = 0; n < 32; n++)
			reg_nums[n] = "$".concat(Integer.toString(n));

		op_names[OP_SPECIAL] = new Name("**spec");
		op_names[OP_REGIMM] = new Name("**regimm");
		op_names[OP_J] = new Name("j", 'J');
		op_names[OP_JAL] = new Name("jal", 'J');
		op_names[OP_BEQ] = new Name("beq", 'r');
		op_names[OP_BNE] = new Name("bne", 'r');
		op_names[OP_BLEZ] = new Name("blez", 'r');
		op_names[OP_BGTZ] = new Name("bgtz", 'r');
		op_names[OP_ADDIU] = new Name("addiu", 'b');
		op_names[OP_SLTI] = new Name("slti", 'b');
		op_names[OP_SLTIU] = new Name("sltiu", 'b');
		op_names[OP_ANDI] = new Name("andi", 'b');
		op_names[OP_ORI] = new Name("ori", 'b');
		op_names[OP_XORI] = new Name("xori", 'b');
		op_names[OP_LUI] = new Name("lui", 'I');
		op_names[OP_COP1] = new Name("**cop1");
		op_names[OP_SPEC2] = new Name("**spec2");
		op_names[OP_LB] = new Name("lb", 'l');
		op_names[OP_LH] = new Name("lh", 'l');
		op_names[OP_LWL] = new Name("lwl", 'l');
		op_names[OP_LW] = new Name("lw", 'l');
		op_names[OP_LBU] = new Name("lbu", 'l');
		op_names[OP_LHU] = new Name("lhu", 'l');
		op_names[OP_LWR] = new Name("lwr", 'l');
		op_names[OP_SB] = new Name("sb", 's');
		op_names[OP_SH] = new Name("sh", 's');
		op_names[OP_SWL] = new Name("swl", 's');
		op_names[OP_SW] = new Name("sw", 's');
		op_names[OP_SWR] = new Name("swr", 's');
		op_names[OP_LWC1] = new Name("lwc1", 'L');
		op_names[OP_SWC1] = new Name("swc1", 'S');

		fn_names[FN_SLL] = new Name("sll");
		fn_names[FN_SRL] = new Name("srl");
		fn_names[FN_SRA] = new Name("sra");
		fn_names[FN_SLLV] = new Name("sllv");
		fn_names[FN_SRLV] = new Name("srlv");
		fn_names[FN_SRAV] = new Name("srav");
		fn_names[FN_JR] = new Name("jr", 'j');
		fn_names[FN_JALR] = new Name("jalr", 'j');
		fn_names[FN_MOVN] = new Name("movn", 'u');
		fn_names[FN_SYSCALL] = new Name("**sys");
		fn_names[FN_BREAK] = new Name("**break");
		fn_names[FN_MFHI] = new Name("mfhi");
		fn_names[FN_MFLO] = new Name("mflo");
		fn_names[FN_MULT] = new Name("mult");
		fn_names[FN_MULTU] = new Name("multu");
		fn_names[FN_DIV] = new Name("div");
		fn_names[FN_DIVU] = new Name("divu");
		fn_names[FN_ADDU] = new Name("addu", 'B');
		fn_names[FN_SUBU] = new Name("subu", 'B');
		fn_names[FN_AND] = new Name("and", 'B');
		fn_names[FN_OR] = new Name("or", 'B');
		fn_names[FN_XOR] = new Name("xor", 'B');
		fn_names[FN_NOR] = new Name("nor", 'B');
		fn_names[FN_SLT] = new Name("slt", 'B');
		fn_names[FN_SLTU] = new Name("sltu", 'B');

		rs_names[RS_MF] = new Name("mfc1", '6');
		rs_names[RS_CF] = new Name("cfc1", '4');
		rs_names[RS_MT] = new Name("mtc1", '5');
		rs_names[RS_CT] = new Name("ctc1");
		rs_names[RS_BC] = new Name("bc1");
		rs_names[RS_FMT_SINGLE] = new Name("**fmts");
		rs_names[RS_FMT_DOUBLE] = new Name("**fmtd");
		rs_names[RS_FMT_WORD] = new Name("**fmtw");

		cfn_names[CFN_ADD_D] = new Name("add", '3');
		cfn_names[CFN_SUB_D] = new Name("sub", '3');
		cfn_names[CFN_MUL_D] = new Name("mul", '3');
		cfn_names[CFN_DIV_D] = new Name("div", '3');
		cfn_names[CFN_MOV_D] = new Name("mov", '2'); // 2?
		cfn_names[CFN_NEG_D] = new Name("neg", '2');
		cfn_names[CFN_CVTS] = new Name("cvts", '2'); // all type 2
		cfn_names[CFN_CVTD] = new Name("cvtd", '2');
		cfn_names[CFN_CVTW] = new Name("cvtw", '2');
		cfn_names[CFN_FC_ULT] = new Name("ult", 'c');
		cfn_names[CFN_FC_EQ] = new Name("eq", 'c'); // all type c
		cfn_names[CFN_FC_LT] = new Name("lt", 'c');
		cfn_names[CFN_FC_LE] = new Name("le", 'c');

		rt_names[RT_BLTZ] = new Name("bltz", 'v');
		rt_names[RT_BGEZ] = new Name("bgez", 'v');
		rt_names[RT_BLTZAL] = new Name("bltzal", 'v');
		rt_names[RT_BGEZAL] = new Name("bgezal", 'v');
	}

	/** return target name for relative branch */
	private static String branchname(Memory m, int isn, int nextpc) {
		short simm = (short) isn;
		return m.getname(nextpc + (simm * 4));
	}

	/**
	 * Dissassemble an instruction prettily e.g.
	 * 4ccf00<main+12> lw $3 = [$29 + 28 : * 4ffff0]
	 * TODO needs to catch addiu reg, 0, imm -> li
	 * TODO show these jump targets
	 * 408f94<__pipe+24>        03e00008  jr       $0, $31, $0
	 * 4004a0<main+c0>          0320f809  jalr     $31, $25, $0
	 */
	static String disasm (Memory m, int[] reg, int pc, int nextpc) {
		if (!m.bound(pc))
			return String.format("disasm: pc not bound: %x", pc);
		int isn = m.load_word(pc);
		int op = isn >>> 26,
		rs = (isn >>> 21) & 0x1f,
		rt = (isn >>> 16) & 0x1f,
		rd = (isn >>> 11) & 0x1f,
		sa = (isn >>> 6) & 0x1f,
		fn = isn & 0x3f;
		short simm = (short) isn;
		int isys = 0, sys = 0; // syscall number if applicable

		String[] regsrc = reg_names; // or reg_nums
		String rss = regsrc[rs];
		String rts = regsrc[rt];
		String rds = regsrc[rd];

		// get name+type (dictates how the parameters are to be formatted)
		String name;
		char type, postfix = 0;
		switch (op) {
			case OP_SPECIAL:
				if (isn == 0) {
					name = "nop";
					type = 'n';
					break;
				}
				switch (fn) {
					case FN_SYSCALL:
						isys = (isn >>> 6) & 0xff;
						// glibc puts syscall in reg 2 instead of instruction
						sys = isys > 0 ? isys : reg[2] - 4000; 
						name = Syscalls.sys_name(sys).name;
						type = 'y';
						break;
					case FN_OR: // catch 'move' instructions
					case FN_ADDU:
						if (rs == 0 || rt == 0) {
							name = "move";
							type = 'm';
							break;
						}
					default:
						name = fn_names[fn].name;
						type = fn_names[fn].type;
						break;
				}
				break;
			case OP_REGIMM:
				name = rt_names[rt].name;
				type = rt_names[rt].type;
				break;
			case OP_COP1: // floating point instruction
				Name n;
				switch (rs) {
					case RS_FMT_SINGLE:
						postfix = 's';
						n = cfn_names[fn];
						break;
					case RS_FMT_DOUBLE:
						postfix = 'd';
						n = cfn_names[fn];
						break;
					case RS_FMT_WORD:
						postfix = 'w';
						n = cfn_names[fn];
						break;
					case RS_BC:
						postfix = ((isn & 10000) != 0) ? 't' : 'f';
					default:
						n = rs_names[rs];
						break;
				}
				name = n.name;
				type = n.type;
				break;
			default:
				name = op_names[op].name;
				type = op_names[op].type;
		}

		String addr, args;

		if (postfix != 0) {
			String pad = ""; // bit of a hack to get the postfix to align
			switch (6 - name.length()) {
				case 1: pad = " "; break;
				case 2: pad = "  "; break;
				case 3: pad = "   "; break;
			}
			addr = String.format("%-24s %08x  %s.%c%s ", m.getname(pc), isn, name, postfix, pad);
		} else {
			addr = String.format("%-24s %08x  %-8s ", m.getname(pc), isn, name);
		}

		int a;

		// <3 printf
		switch (type) {
			case 'R': // generic register
				if (sa > 0)
					args = String.format("%s, %s, %s shift %d", rds, rss, rts, sa);
				else
					args = String.format("%s, %s, %s", rds, rss, rts);
				break;
			case 'I': // generic immediate
				args = String.format("%s, %s, %+d", rts, rss, simm); break;
			case 'J': // jump instruction. show target
				args = m.getname((isn << 6) >>> 4); break;
			case 'j': // jr or jalr, show target
				args = String.format("%s, %s : %s", rds, rss, m.getname(reg[rs]));
				break;
			case 'r': // rel branch
				args = String.format("%s * %s, to %s", rts, rss, branchname(m, isn, nextpc)); break;
			case 'v': // relative branch 1 operand (eg bltz)
				args = String.format("%s : %d to %s", rss, reg[rs], branchname(m, isn, nextpc)); break;
			case 'y': // syscall
				args = String.format("syscall %d service %d", isys, sys); break;
			case 'B': // binary register function
				args = String.format("%s = %s * %s", rds, rss, rts); break;
			case 'b': // binary immediate function
				args = String.format("%s = %s * %+d", rts, rss, simm); break;
			case 'l': // load int from mem
				a = reg[rs] + simm;
				// use mem.bound in case the emulator crashed on this instruction
				args = String.format("%s = [%s %+d] : %s = [%s]", rts, rss, simm, m.bound(a) ? m.getname(m.load_word(a)) : "null", m.getname(a)); break;
			case 'L': // lwc1
				a = reg[rs] + simm;
				args = String.format("#%d = [%s %+d] : %s = [%s]", rt, rss, simm,  m.bound(a) ? m.getname(m.load_word(a)) : "null", m.getname(a)); break;
			case 's': // store int to mem
				args = String.format("[%s %+d] = %s : [%s] = %s", rss, simm, rts, m.getname(reg[rs] + simm), m.getname(reg[rt])); break;
			case 'S': // swc1 (like store)
				args = String.format("[%s %+d] = #%d : [%s] = %x", rss, simm, rt, m.getname(reg[rs] + simm), reg[rt]); break;
			case 'n': // nop
				args = ""; break;
			case 'm': // move
				args = String.format("%s = %s", rds, rs > rt ? rss : rts); break;
			case 'c': // floating point compare
				args = String.format("#%d ? #%d", rd, rt); break;
			case '2': // floating point 2 operand
				args = String.format("#%d = * #%d", sa, rd); break;
			case '3': // floating point 3 operand
				args = String.format("#%d = #%d * #%d", sa, rd, rt); break;
			case '4': // cfc1
				args = String.format("%s = ctrl(%d)", rss, rd); break;
			case '5': // mtc1
				args = String.format("#%d = %s", rd, rts); break;
			case '6': // mfc1
				args = String.format("%s = #%d", rts, rd); break;
			default: // go nuts
				args = String.format("op=%d rs,fmt=%d rt,ft=%d rd,fs=%d sa,fd=%d fn=%d simm=%d", op, rs, rt, rd, sa, fn, simm);
		}

		return addr.concat(args);
	}

	/**
	 * Get the offset of the return address slot from the stack pointer for the
	 * given function. E.g. returns 32 for "sw [sp +32] = ra". Returns -1 if there
	 * is no return slot (i.e. a leaf function).
	 */
	static int findraslot(Memory mem, int addr) {
		// afbf0020  sw       [sp +32] = ra
		for (int n = 0; n < 8; n++) {
			int isn = mem.load_word(addr + (n * 4));
			if ((isn & 0xffff0000) == 0xafbf0000) {
				short imm = (short) isn;
				return imm;
			}
		}
		return -1;
	}

	/**
	 * Get the frame size of the given function. E.g. returns 64 for "addiu sp =
	 * sp * -64". Return -1 if there is no frame.
	 */
	static int findframesize(Memory mem, int addr) {
		// 27bdffc0  addiu    sp = sp * -64
		for (int n = 0; n < 8; n++) {
			int isn = mem.load_word(addr + (n * 4));
			if ((isn & 0xffff0000) == 0x27bd0000) {
				short imm = (short) -isn;
				return imm;
			}
		}
		// happens for hlt
		//opt.println("address but no frame for %s", f);
		return -1;
	}

} // end of class MipsIsn
