package ocelot;

import static ocelot.JVMValue.entryRef;
import ocelot.classfile.OtKlassParser;
import ocelot.rt.*;

/**
 *
 * @author ben
 */
public final class InterpMain {

    private static final Opcode[] table = new Opcode[256];

    // Singleton ?
    private static SharedKlassRepo repo = SharedKlassRepo.of();

    public InterpMain() {
    }

    public InterpMain(SharedKlassRepo classes) {
        repo = classes;
    }

    static {
        for (Opcode op : Opcode.values()) {
            table[op.getOpcode()] = op;
        }
        // Sanity check
        int count = 0;
        for (int i = 0; i < 256; i++) {
            if (table[i] != null)
                count++;
        }
        final int numOpcodes = Opcode.values().length;
        if (count != numOpcodes) {
            throw new IllegalStateException("Opcode sanity check failed: " + count + " opcodes found, should be " + numOpcodes);
        }
    }

    public static SharedKlassRepo getRepo() {
        return repo;
    }

    public JVMValue execMethod(final OtMethod meth) {
        return execMethod(meth.getClassName(), meth.getNameAndType(), meth.getBytecode(), new InterpLocalVars());
    }

    public JVMValue execMethod(final OtMethod meth, final InterpLocalVars lvt) {
        return execMethod(meth.getClassName(), meth.getNameAndType(), meth.getBytecode(), lvt);
    }

    JVMValue execMethod(final String klassName, final String desc, final byte[] instr, final InterpLocalVars lvt) {
        if (instr == null || instr.length == 0)
            return null;

        final InterpEvalStack eval = new InterpEvalStack();

        int current = 0;
        LOOP:
        while (true) {
            byte b = instr[current++];
            Opcode op = table[b & 0xff];
            if (op == null) {
                System.err.println("Unrecognised opcode byte: " + (b & 0xff) + " encountered at position " + (current - 1) + ". Stopping.");
                System.exit(1);
            }
            byte num = op.numParams();
            JVMValue v, v2, ret;
            OtKlassParser.CPMethod toBeCalled;
            int paramCount, jumpTo, cpLookup;
            InterpLocalVars withVars;
            JVMValue[] toPass;
            switch (op) {
                case ACONST_NULL:
                    eval.aconst_null();
                    break;
                case ALOAD:
                    eval.push(lvt.aload(instr[current++]));
                    break;
                case ALOAD_0:
                    eval.push(lvt.aload((byte) 0));
                    break;
                case ALOAD_1:
                    eval.push(lvt.aload((byte) 1));
                    break;
                case ARETURN:
                    return eval.pop();
                case ASTORE:
                    lvt.astore(instr[current++], eval.pop());
                    break;
                case ASTORE_0:
                    lvt.astore((byte) 0, eval.pop());
                    break;
                case ASTORE_1:
                    lvt.astore((byte) 1, eval.pop());
                    break;
                case BIPUSH:
                    eval.iconst((int) instr[current++]);
                    break;
                case DUP:
                    eval.dup();
                    break;
                case DUP_X1:
                    eval.dupX1();
                    break;
                case GOTO:
                    current += 2 + ((int) instr[current] << 8) + (int) instr[current + 1];
                    break;
                case I2D:
                    eval.i2d();
                    break;
                case IADD:
                    eval.iadd();
                    break;
                case IAND:
                    eval.iand();
                    break;
                case ICONST_0:
                    eval.iconst(0);
                    break;
                case ICONST_1:
                    eval.iconst(1);
                    break;
                case ICONST_2:
                    eval.iconst(2);
                    break;
                case ICONST_3:
                    eval.iconst(3);
                    break;
                case ICONST_4:
                    eval.iconst(4);
                    break;
                case ICONST_5:
                    eval.iconst(5);
                    break;
                case ICONST_M1:
                    eval.iconst(-1);
                    break;
                case IDIV:
                    eval.idiv();
                    break;
                case IF_ICMPEQ:
                    v = eval.pop();
                    v2 = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value == v2.value) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFEQ:
                    v = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value == 0L) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFGE:
                    v = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value >= 0L) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFGT:
                    v = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value > 0L) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFLE:
                    v = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value <= 0L) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFLT:
                    v = eval.pop();
                    jumpTo = ((int) instr[current++] << 8) + (int) instr[current++];
                    if (v.value < 0L) {
                        current += jumpTo - 1; // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFNE:
                    v = eval.pop();
                    jumpTo = ((int) instr[current] << 8) + (int) instr[current + 1];
                    if (v.value != 0L) {
                        current += jumpTo - 1;  // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFNONNULL:
                    v = eval.pop();
                    jumpTo = ((int) instr[current] << 8) + (int) instr[current + 1];
                    // FIXME Check that this is of reference type
                    if (v.value != 0L) {
                        current += jumpTo - 1;  // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IFNULL:
                    v = eval.pop();
                    jumpTo = ((int) instr[current] << 8) + (int) instr[current + 1];
                    // FIXME Check that this is of reference type
                    if (v.value == 0L) {
                        current += jumpTo - 1;  // The -1 is necessary as we've already inc'd current
                    }
                    break;
                case IINC:
                    lvt.iinc(instr[current++], instr[current++]);
                    break;
                case ILOAD:
                    eval.push(lvt.iload(instr[current++]));
                    break;
                case ILOAD_0:
                    eval.push(lvt.iload((byte) 0));
                    break;
                case ILOAD_1:
                    eval.push(lvt.iload((byte) 1));
                    break;
                case ILOAD_2:
                    eval.push(lvt.iload((byte) 2));
                    break;
                case ILOAD_3:
                    eval.push(lvt.iload((byte) 3));
                    break;
                case IMUL:
                    eval.imul();
                    break;
                case INEG:
                    eval.ineg();
                    break;
                case IOR:
                    eval.ior();
                    break;
                case IREM:
                    eval.irem();
                    break;
                case IRETURN:
                    return eval.pop();
                case ISTORE:
                    lvt.store(instr[current++], eval.pop());
                    break;
                case ISTORE_0:
                    lvt.store((byte) 0, eval.pop());
                    break;
                case ISTORE_1:
                    lvt.store((byte) 1, eval.pop());
                    break;
                case ISTORE_2:
                    lvt.store((byte) 2, eval.pop());
                    break;
                case ISTORE_3:
                    lvt.store((byte) 3, eval.pop());
                    break;
                case ISUB:
                    eval.isub();
                    break;
                case NOP:
                    break;
                case POP:
                    eval.pop();
                    break;
                case POP2:
                    JVMValue discard = eval.pop();
                    if (discard.type == JVMType.J || discard.type == JVMType.D) {
                        break;
                    }
                    eval.pop();
                    break;
                case RETURN:
                    return null;
                case SIPUSH:
                    eval.iconst(((int) instr[current++] << 8) + (int) instr[current++]);
                    break;
                case SWAP:
                    JVMValue val1 = eval.pop();
                    JVMValue val2 = eval.pop();
                    eval.push(val1);
                    eval.push(val2);
                    break;
                // Dummy implementation
                case LDC:
                    System.out.print("Executing " + op + " with param bytes: ");
                    for (int i = current; i < current + num; i++) {
                        System.out.print(instr[i] + " ");
                    }
                    current += num;
                    System.out.println();
                    break;
                // Disallowed opcodes
                case BREAKPOINT:
                case IMPDEP1:
                case IMPDEP2:
                case JSR:
                case JSR_W:
                case RET:
                    throw new IllegalArgumentException("Illegal opcode byte: " + (b & 0xff) + " encountered at position " + (current - 1) + ". Stopping.");
                default:
                    System.err.println("Saw " + op + " - that can't happen. Stopping.");
                    System.exit(1);
            }
            // SAFEPOINT CHECK GOES HERE
        }
    }

    public void dispatchInvoke(OtMethod toBeCalled, InterpEvalStack eval) {
        int paramCount = toBeCalled.numParams();
        if (!toBeCalled.isStatic())
            paramCount++;
        InterpLocalVars withVars = new InterpLocalVars();
        JVMValue[] toPass = new JVMValue[paramCount];
        for (int j = paramCount - 1; j >= 0; j--) {
            toPass[j] = eval.pop();
        }
        withVars.setup(toPass);

        JVMValue val = execMethod(toBeCalled, withVars);
        if (val != null)
            eval.push(val);
    }

}
