package Codegen;

import Assem.*;
import Temp.*;
import Tree.*;
import Mips.Frame;
import java.util.ArrayList;

public class Codegen {
    private InstrList ilist = null, last = null;
    private final Frame frame;
    
    public Codegen(Frame f) {
        frame = f;
    }
    
    private void emit(Instr inst) {
        if (last != null) {
            last = last.tail = new InstrList(inst, null);
        } else {
            last = ilist = new InstrList(inst, null);
        }
    }
    
    public InstrList codegen(StmList stms) {
        ilist = last = null;
        for (StmList l = stms; l != null; l = l.tail) {
            munchStm(l.head);
        }
        return ilist;
    }

    private void munchStm(Stm s) {
        if (s instanceof Tree.LABEL) {
            Tree.LABEL l = (Tree.LABEL) s;
            emit(new Assem.LABEL(l.label.toString() + ":\n", l.label));
        } else if (s instanceof JUMP) {
            JUMP j = (JUMP) s;
            if (j.exp instanceof NAME) {
                NAME n = (NAME) j.exp;
                emit(new OPER("j `j0\n", null, null, new LabelList(n.label, null)));
            } else {
                Temp t = munchExp(j.exp);
                emit(new OPER("jr `s0\n", null, new TempList(t, null)));
            }
        } else if (s instanceof CJUMP) {
            CJUMP c = (CJUMP) s;
            Temp a = munchExp(c.left);
            Temp b = munchExp(c.right);
            String op = branchOp(c.relop);
            emit(new OPER(op + " `s0, `s1, `j0\n", null, new TempList(a, new TempList(b, null)), new LabelList(c.iftrue, null)));
        } else if (s instanceof Tree.MOVE) {
            Tree.MOVE m = (Tree.MOVE) s;
            munchMove(m.dst, m.src);
        } else if (s instanceof EXP_stm) {
            EXP_stm e = (EXP_stm) s;
            munchExp(e.exp);
        } else {
            throw new Error("Unsupported statement in Codegen: " + s.getClass());
        }
    }

    private void munchMove(Exp dst, Exp src) {
        if (dst instanceof TEMP && src instanceof CALL) {
            munchCall((CALL) src, ((TEMP) dst).temp);
        } else if (dst instanceof TEMP) {
            Temp d = ((TEMP) dst).temp;
            Temp s = munchExp(src);
            emit(new Assem.MOVE("move `d0, `s0\n", d, s));
        } else if (dst instanceof MEM) {
            MEM m = (MEM) dst;
            Addr a = munchAddr(m.exp);
            Temp s = munchExp(src);
            emit(new OPER("sw `s0, " + a.off + "(`s1)\n", null, new TempList(s, new TempList(a.base, null))));
        } else {
            throw new Error("Invalid move destination: " + dst.getClass());
        }
    }

    private Temp munchExp(Exp e) {
        if (e instanceof TEMP) {
            return ((TEMP) e).temp;
        } else if (e instanceof CONST) {
            CONST c = (CONST) e;
            Temp t = new Temp();
            emit(new OPER("li `d0, " + c.value + "\n", new TempList(t, null), null));
            return t;
        } else if (e instanceof NAME) {
            NAME n = (NAME) e;
            Temp t = new Temp();
            emit(new OPER("la `d0, " + n.label.toString() + "\n", new TempList(t, null), null));
            return t;
        } else if (e instanceof MEM) {
            MEM m = (MEM) e;
            Addr a = munchAddr(m.exp);
            Temp t = new Temp();
            emit(new OPER("lw `d0, " + a.off + "(`s0)\n", new TempList(t, null), new TempList(a.base, null)));
            return t;
        } else if (e instanceof BINOP) {
            BINOP b = (BINOP) e;
            Temp t = new Temp();
            if (b.binop == BINOP.PLUS && b.right instanceof CONST) {
                Temp left = munchExp(b.left);
                int val = ((CONST) b.right).value;
                emit(new OPER("addi `d0, `s0, " + val + "\n", new TempList(t, null), new TempList(left, null)));
            } else if (b.binop == BINOP.PLUS && b.left instanceof CONST) {
                Temp right = munchExp(b.right);
                int val = ((CONST) b.left).value;
                emit(new OPER("addi `d0, `s0, " + val + "\n", new TempList(t, null), new TempList(right, null)));
            } else {
                Temp left = munchExp(b.left);
                Temp right = munchExp(b.right);
                String op = null;
                switch (b.binop) {
                    case BINOP.PLUS: op = "add"; break;
                    case BINOP.MINUS: op = "sub"; break;
                    case BINOP.MUL: op = "mul"; break;
                    case BINOP.DIV: op = "div"; break;
                    case BINOP.AND: op = "and"; break;
                    case BINOP.OR:  op = "or";  break;
                    case BINOP.XOR: op = "xor"; break;
                    case BINOP.LSHIFT:  op = "sllv"; break;
                    case BINOP.RSHIFT:  op = "srlv"; break;
                    case BINOP.ARSHIFT: op = "srav"; break;
                }
                emit(new OPER(op + " `d0, `s0, `s1\n", new TempList(t, null), new TempList(left, new TempList(right, null))));
            }
            return t;
        } else if (e instanceof CALL) {
            return munchCall((CALL) e, null);
        } else if (e instanceof ESEQ) {
            ESEQ es = (ESEQ) e;
            munchStm(es.stm);
            return munchExp(es.exp);
        } else {
            throw new Error("Unsupported Exp in Codegen: " + e.getClass());
        }
    }

    private Temp munchCall(CALL call, Temp resOpt) {
        ArrayList<Temp> argTemps = new ArrayList<>();
        for (ExpList a = call.args; a != null; a = a.tail) {
            argTemps.add(munchExp(a.head));
        }
        
        int regArgs = Math.min(argTemps.size(), Frame.numArgRegs());
        int stackArgs = argTemps.size() - regArgs;
        int stackAdjustment = stackArgs * Frame.wordSize();
        
        if (stackAdjustment > 0) {
            TempList spList = new TempList(Frame.SP, null);
            emit(new OPER("addi `d0, `s0, -" + stackAdjustment + "\n", spList, spList));
        }
        
        for (int i = 0; i < regArgs; i++) {
            Temp src = argTemps.get(i);
            Temp dst = Frame.argReg(i);
            emit(new Assem.MOVE("move `d0, `s0\n", dst, src));
        }
        
        for (int i = 0; i < stackArgs; i++) {
            Temp src = argTemps.get(regArgs + i);
            int offset = i * Frame.wordSize();
            emit(new OPER("sw `s0, " + offset + "(`s1)\n", null, new TempList(src, new TempList(Frame.SP, null))));
        }
        
        TempList callDefs = Frame.callerSaves();
        callDefs = new TempList(Frame.RV2, callDefs);
        callDefs = new TempList(Frame.RV,  callDefs);
        callDefs = new TempList(Frame.RA,  callDefs);
        callDefs = new TempList(Frame.argReg(0), callDefs);
        callDefs = new TempList(Frame.argReg(1), callDefs);
        callDefs = new TempList(Frame.argReg(2), callDefs);
        callDefs = new TempList(Frame.argReg(3), callDefs);
        
        TempList callUses = null;
        for (int i = regArgs - 1; i >= 0; i--) {
            callUses = new TempList(Frame.argReg(i), callUses);
        }
        
        if (call.func instanceof NAME) {
            String lbl = ((NAME) call.func).label.toString();
            emit(new OPER("jal " + lbl + "\n", callDefs, callUses));
        } else {
            Temp f = munchExp(call.func);
            callUses = new TempList(f, callUses);
            emit(new OPER("jalr `s0\n", callDefs, callUses));
        }
        
        if (stackAdjustment > 0) {
            TempList spList = new TempList(Frame.SP, null);
            emit(new OPER("addi `d0, `s0, " + stackAdjustment + "\n", spList, spList));
        }
        
        if (resOpt != null) {
            emit(new Assem.MOVE("move `d0, `s0\n", resOpt, Frame.RV));
            return resOpt;
        } else {
            Temp r = new Temp();
            emit(new Assem.MOVE("move `d0, `s0\n", r, Frame.RV));
            return r;
        }
    }

    private static class Addr {
        final Temp base;
        final int off;
        Addr(Temp b, int o) {
            base = b;
            off = o;
        }
    }

    private Addr munchAddr(Exp e) {
        if (e instanceof BINOP) {
            BINOP b = (BINOP) e;
            if (b.binop == BINOP.PLUS) {
                if (b.left instanceof CONST) {
                    int k = ((CONST) b.left).value;
                    return new Addr(munchExp(b.right), k);
                } else if (b.right instanceof CONST) {
                    int k = ((CONST) b.right).value;
                    return new Addr(munchExp(b.left), k);
                }
            }
        }
        return new Addr(munchExp(e), 0);
    }

    private String branchOp(int rel) {
        switch (rel) {
            case CJUMP.EQ:  return "beq";
            case CJUMP.NE:  return "bne";
            case CJUMP.LT:  return "blt";
            case CJUMP.GT:  return "bgt";
            case CJUMP.LE:  return "ble";
            case CJUMP.GE:  return "bge";
            case CJUMP.ULT: return "bltu";
            case CJUMP.ULE: return "bleu";
            case CJUMP.UGT: return "bgtu";
            case CJUMP.UGE: return "bgeu";
            default: throw new Error("unknown relop");
        }
    }
}
