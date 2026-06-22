package visitor;

import java.util.*;
import syntaxtree.*;
import Temp.*;

public class IRTranslator implements Visitor {
    private final Deque<HashMap<String, Temp>> envStack = new ArrayDeque<>();
    private final Deque<HashMap<String, String>> typeEnvStack = new ArrayDeque<>();
    private Tree.Stm resultStm; // last statement result
    private Tree.Exp resultExp; // last expression result
    private final ArrayList<ProcedureIR> procedures = new ArrayList<>();
    private final HashMap<String, ArrayList<String>> classFields = new HashMap<>();
    private final HashMap<String, HashMap<String, Integer>> fieldOffsets = new HashMap<>();
    private final HashMap<String, HashMap<String, String>> classAllFieldTypes = new HashMap<>();
    private final HashMap<String, String> classParents = new HashMap<>();
    private final HashMap<String, HashMap<String, String>> classMethodTypes = new HashMap<>();
    private static final int WORD_SIZE = 4;
    private String currentClassName = null;
    public static boolean DEBUG = false;

    // Inner class representing procedure intermediate representation
    public static class ProcedureIR {
        public final String name;
        public final Mips.Frame frame;
        public final Tree.Stm body;

        public ProcedureIR(String name, Mips.Frame frame, Tree.Stm body) {
            this.name = name;
            this.frame = frame;
            this.body = body;
        }
    }

    private HashMap<String, Temp> env() { return envStack.peek(); }
    private HashMap<String, String> typeEnv() { return typeEnvStack.peek(); }

    private void envPush() {
        envStack.push(envStack.isEmpty() ? new HashMap<>() : new HashMap<>(envStack.peek()));
        typeEnvStack.push(typeEnvStack.isEmpty() ? new HashMap<>() : new HashMap<>(typeEnvStack.peek()));
    }

    private void envPop() {
        envStack.pop();
        typeEnvStack.pop();
    }

    private Temp tempOf(String name) {
        Temp t = env().get(name);
        if (t == null) {
            t = new Temp();
            env().put(name, t);
        }
        return t;
    }

    private String typeNameOf(syntaxtree.Type t) {
        if (t instanceof syntaxtree.IntegerType) return "int";
        if (t instanceof syntaxtree.BooleanType) return "boolean";
        if (t instanceof syntaxtree.IntArrayType) return "int[]";
        if (t instanceof syntaxtree.IdentifierType) return ((syntaxtree.IdentifierType) t).s;
        return "void";
    }

    private String methodLabel(String className, String methodName) {
        return className + "_" + methodName;
    }

    private ArrayList<String> getOrBuildFields(String cname, HashMap<String, syntaxtree.ClassDecl> classDecls) {
        if (classFields.containsKey(cname)) {
            return classFields.get(cname);
        }
        ArrayList<String> fields = new ArrayList<>();
        String parent = classParents.get(cname);
        if (parent != null) {
            fields.addAll(getOrBuildFields(parent, classDecls));
        }

        syntaxtree.ClassDecl cd = classDecls.get(cname);
        if (cd != null) {
            syntaxtree.VarDeclList vdl = null;
            if (cd instanceof syntaxtree.ClassDeclSimple) {
                vdl = ((syntaxtree.ClassDeclSimple) cd).vl;
            } else if (cd instanceof syntaxtree.ClassDeclExtends) {
                vdl = ((syntaxtree.ClassDeclExtends) cd).vl;
            }
            if (vdl != null) {
                for (int i = 0; i < vdl.size(); i++) {
                    String fname = vdl.elementAt(i).i.s;
                    fields.add(fname);
                    String ftype = typeNameOf(vdl.elementAt(i).t);
                    if (!classAllFieldTypes.containsKey(cname)) {
                        classAllFieldTypes.put(cname, new HashMap<>());
                    }
                    classAllFieldTypes.get(cname).put(fname, ftype);
                }
            }
        }

        if (parent != null) {
            HashMap<String, String> parentTypes = classAllFieldTypes.get(parent);
            if (parentTypes != null) {
                if (!classAllFieldTypes.containsKey(cname)) {
                    classAllFieldTypes.put(cname, new HashMap<>());
                }
                classAllFieldTypes.get(cname).putAll(parentTypes);
            }
        }

        classFields.put(cname, fields);

        HashMap<String, Integer> offsets = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            offsets.put(fields.get(i), i * WORD_SIZE);
        }
        fieldOffsets.put(cname, offsets);

        return fields;
    }

    private void preScanClassLayouts(syntaxtree.Program n) {
        HashMap<String, syntaxtree.ClassDecl> classDecls = new HashMap<>();

        for (int i = 0; i < n.cl.size(); i++) {
            syntaxtree.ClassDecl cd = n.cl.elementAt(i);
            String cname = null;
            if (cd instanceof syntaxtree.ClassDeclSimple) {
                cname = ((syntaxtree.ClassDeclSimple) cd).i.s;
            } else if (cd instanceof syntaxtree.ClassDeclExtends) {
                cname = ((syntaxtree.ClassDeclExtends) cd).i.s;
                classParents.put(cname, ((syntaxtree.ClassDeclExtends) cd).j.s);
            }
            if (cname != null) {
                classDecls.put(cname, cd);
            }
        }

        for (String cname : classDecls.keySet()) {
            getOrBuildFields(cname, classDecls);
        }

        for (int i = 0; i < n.cl.size(); i++) {
            syntaxtree.ClassDecl cd = n.cl.elementAt(i);
            String cname = (cd instanceof syntaxtree.ClassDeclSimple) ?
                ((syntaxtree.ClassDeclSimple) cd).i.s : ((syntaxtree.ClassDeclExtends) cd).i.s;

            syntaxtree.MethodDeclList mdl = (cd instanceof syntaxtree.ClassDeclSimple) ?
                ((syntaxtree.ClassDeclSimple) cd).ml : ((syntaxtree.ClassDeclExtends) cd).ml;

            HashMap<String, String> mtypes = new HashMap<>();
            String parent = classParents.get(cname);
            if (parent != null) {
                HashMap<String, String> parentMtypes = classMethodTypes.get(parent);
                if (parentMtypes != null) {
                    mtypes.putAll(parentMtypes);
                }
            }

            for (int j = 0; j < mdl.size(); j++) {
                syntaxtree.MethodDecl md = mdl.elementAt(j);
                mtypes.put(md.i.s, typeNameOf(md.t));
            }
            classMethodTypes.put(cname, mtypes);
        }
    }

    private Tree.Exp varExp(String name) {
        Temp t = env().get(name);
        if (t != null) {
            return new Tree.TEMP(t);
        }

        if (currentClassName != null) {
            HashMap<String, Integer> offsets = fieldOffsets.get(currentClassName);
            if (offsets != null && offsets.containsKey(name)) {
                int off = offsets.get(name);
                Temp thisTemp = env().get("this");
                if (thisTemp != null) {
                    return new Tree.MEM(bin(Tree.BINOP.PLUS, new Tree.TEMP(thisTemp), new Tree.CONST(off)));
                }
            }
        }

        return new Tree.TEMP(tempOf(name));
    }

    private Tree.Stm assignVar(String name, Tree.Exp rhs) {
        Temp t = env().get(name);
        if (t != null) {
            return new Tree.MOVE(new Tree.TEMP(t), rhs);
        }

        if (currentClassName != null) {
            HashMap<String, Integer> offsets = fieldOffsets.get(currentClassName);
            if (offsets != null && offsets.containsKey(name)) {
                int off = offsets.get(name);
                Temp thisTemp = env().get("this");
                if (thisTemp != null) {
                    return new Tree.MOVE(new Tree.MEM(bin(Tree.BINOP.PLUS, new Tree.TEMP(thisTemp), new Tree.CONST(off))), rhs);
                }
            }
        }

        return new Tree.MOVE(new Tree.TEMP(tempOf(name)), rhs);
    }

    private Tree.Stm print1(Tree.Exp e) {
        return new Tree.EXP_stm(new Tree.CALL(new Tree.NAME(new Label(Mips.Frame.printLabel())), new Tree.ExpList(e, null)));
    }

    private Tree.Stm maybeSeq(List<Tree.Stm> stms) {
        if (stms.isEmpty()) {
            return new Tree.EXP_stm(new Tree.CONST(0));
        }
        Tree.Stm res = stms.get(0);
        for (int i = 1; i < stms.size(); i++) {
            res = new Tree.SEQ(res, stms.get(i));
        }
        return res;
    }

    private Tree.Stm seq(Tree.Stm... args) {
        if (args.length == 0) return null;
        Tree.Stm res = args[0];
        for (int i = 1; i < args.length; i++) {
            res = seqHelper(res, args[i]);
        }
        return res;
    }

    private Tree.Stm seqHelper(Tree.Stm a, Tree.Stm b) {
        if (a == null) return b;
        if (b == null) return a;
        return new Tree.SEQ(a, b);
    }

    private Tree.Stm seq(List<Tree.Stm> args) {
        if (args.isEmpty()) return null;
        Tree.Stm res = args.get(0);
        for (int i = 1; i < args.size(); i++) {
            res = seqHelper(res, args.get(i));
        }
        return res;
    }

    private Tree.Exp bin(int op, Tree.Exp left, Tree.Exp right) {
        return new Tree.BINOP(op, left, right);
    }

    private Tree.Exp const0() { return new Tree.CONST(0); }
    private Tree.Exp const1() { return new Tree.CONST(1); }

    private Tree.ExpList append(Tree.ExpList list, Tree.Exp item) {
        if (list == null) return new Tree.ExpList(item, null);
        Tree.ExpList cur = list;
        while (cur.tail != null) {
            cur = cur.tail;
        }
        cur.tail = new Tree.ExpList(item, null);
        return list;
    }

    private String receiverClassOf(syntaxtree.Exp recv) {
        if (recv instanceof syntaxtree.This) {
            return currentClassName;
        } else if (recv instanceof syntaxtree.NewObject) {
            return ((syntaxtree.NewObject) recv).i.s;
        } else if (recv instanceof syntaxtree.IdentifierExp) {
            return typeEnv().get(((syntaxtree.IdentifierExp) recv).s);
        } else if (recv instanceof syntaxtree.Call) {
            syntaxtree.Call call = (syntaxtree.Call) recv;
            String rc = receiverClassOf(call.e);
            if (rc != null) {
                HashMap<String, String> methods = classMethodTypes.get(rc);
                if (methods != null) {
                    return methods.get(call.i.s);
                }
            }
        }
        return null;
    }

    public List<ProcedureIR> translate(syntaxtree.Program root) {
        procedures.clear();
        envPush();
        root.accept(this);
        envPop();
        return new ArrayList<>(procedures);
    }

    // Visitor Interface implementations with FQCNs to prevent naming conflicts with Tree package
    public void visit(syntaxtree.Program n) {
        preScanClassLayouts(n);
        n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.elementAt(i).accept(this);
        }
    }

    public void visit(syntaxtree.MainClass n) {
        envPush();
        List<Tree.Stm> stms = new ArrayList<>();
        n.s.accept(this);
        if (resultStm != null) stms.add(resultStm);

        Mips.Frame frame = new Mips.Frame("main", null);
        resultStm = frame.procEntryExit1(maybeSeq(stms));
        procedures.add(new ProcedureIR("main", frame, resultStm));
        envPop();
    }

    public void visit(syntaxtree.ClassDeclSimple n) {
        String savedClass = currentClassName;
        currentClassName = n.i.s;

        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.elementAt(i).accept(this);
        }
        currentClassName = savedClass;
    }

    public void visit(syntaxtree.ClassDeclExtends n) {
        String savedClass = currentClassName;
        currentClassName = n.i.s;

        for (int i = 0; i < n.ml.size(); i++) {
            n.ml.elementAt(i).accept(this);
        }
        currentClassName = savedClass;
    }

    public void visit(syntaxtree.VarDecl n) {
        // Handled in MethodDecl / ClassLayout scanner
    }

    public void visit(syntaxtree.Formal n) {
        // Handled in MethodDecl binder
    }

    public void visit(syntaxtree.MethodDecl n) {
        envPush();
        Util.BoolList escapes = null;
        for (int j = n.fl.size(); j >= 0; j--) {
            escapes = new Util.BoolList(false, escapes);
        }

        String mlabel = methodLabel(currentClassName, n.i.s);
        Mips.Frame frame = new Mips.Frame(mlabel, escapes);
        List<Tree.Stm> stms = new ArrayList<>();
        List<Mips.Access> formals = frame.getFormals();
        int formalIndex = 0;

        // 1. Implicit 'this'
        Mips.Access thisAcc = formals.get(formalIndex++);
        if (thisAcc instanceof Mips.InFrame) {
            env().put("this", new Temp());
        } else if (thisAcc instanceof Mips.InReg) {
            env().put("this", ((Mips.InReg) thisAcc).temp);
        }
        typeEnv().put("this", currentClassName);

        // 2. Formals binding
        for (int j = 0; j < n.fl.size(); j++) {
            String name = n.fl.elementAt(j).i.s;
            Mips.Access acc = formals.get(formalIndex++);
            if (acc instanceof Mips.InFrame) {
                env().put(name, new Temp());
            } else if (acc instanceof Mips.InReg) {
                env().put(name, ((Mips.InReg) acc).temp);
            }
            typeEnv().put(name, typeNameOf(n.fl.elementAt(j).t));
        }

        // 3. Locals
        for (int j = 0; j < n.vl.size(); j++) {
            syntaxtree.VarDecl vd = n.vl.elementAt(j);
            tempOf(vd.i.s);
            typeEnv().put(vd.i.s, typeNameOf(vd.t));
        }

        // 4. Statements
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.elementAt(i).accept(this);
            if (resultStm != null) stms.add(resultStm);
        }

        // 5. Return value
        n.e.accept(this);
        Tree.Exp ret = resultExp;
        stms.add(new Tree.MOVE(new Tree.TEMP(Mips.Frame.RV), ret));

        // 6. ProcEntryExit1 encapsulate
        resultStm = frame.procEntryExit1(maybeSeq(stms));
        procedures.add(new ProcedureIR(mlabel, frame, resultStm));
        envPop();
    }

    public void visit(syntaxtree.IntArrayType n) {}
    public void visit(syntaxtree.BooleanType n) {}
    public void visit(syntaxtree.IntegerType n) {}
    public void visit(syntaxtree.IdentifierType n) {}

    public void visit(syntaxtree.Block n) {
        List<Tree.Stm> stms = new ArrayList<>();
        for (int i = 0; i < n.sl.size(); i++) {
            n.sl.elementAt(i).accept(this);
            if (resultStm != null) stms.add(resultStm);
        }
        resultStm = maybeSeq(stms);
    }

    public void visit(syntaxtree.If n) {
        n.e.accept(this);
        Tree.Exp cond = resultExp;
        Label lt = new Label();
        Label lf = new Label();
        Label le = new Label();

        Tree.CJUMP c = new Tree.CJUMP(Tree.CJUMP.NE, cond, const0(), lt, lf);
        n.s1.accept(this);
        Tree.Stm s1 = resultStm;
        n.s2.accept(this);
        Tree.Stm s2 = resultStm;

        resultStm = seq(c, new Tree.LABEL(lt), s1, new Tree.JUMP(le), new Tree.LABEL(lf), s2, new Tree.LABEL(le));
    }

    public void visit(syntaxtree.While n) {
        Label test = new Label();
        Label body = new Label();
        Label end = new Label();

        n.e.accept(this);
        Tree.Exp cond = resultExp;
        n.s.accept(this);
        Tree.Stm s = resultStm;

        resultStm = seq(
            new Tree.LABEL(test),
            new Tree.CJUMP(Tree.CJUMP.NE, cond, const0(), body, end),
            new Tree.LABEL(body),
            s,
            new Tree.JUMP(test),
            new Tree.LABEL(end)
        );
    }

    public void visit(syntaxtree.Print n) {
        n.e.accept(this);
        resultStm = print1(resultExp);
    }

    public void visit(syntaxtree.Assign n) {
        n.e.accept(this);
        resultStm = assignVar(n.i.s, resultExp);
    }

    public void visit(syntaxtree.ArrayAssign n) {
        n.e1.accept(this);
        Tree.Exp idx = resultExp;
        n.e2.accept(this);
        Tree.Exp val = resultExp;
        Tree.Exp base = varExp(n.i.s);
        // Base points to length at 0, data starts at offset 4
        Tree.Exp addr = bin(Tree.BINOP.PLUS, base, bin(Tree.BINOP.PLUS, new Tree.CONST(4), bin(Tree.BINOP.MUL, idx, new Tree.CONST(4))));
        resultStm = new Tree.MOVE(new Tree.MEM(addr), val);
    }

    public void visit(syntaxtree.And n) {
        n.e1.accept(this);
        Tree.Exp a = resultExp;
        n.e2.accept(this);
        Tree.Exp b = resultExp;
        resultExp = bin(Tree.BINOP.AND, a, b);
    }

    public void visit(syntaxtree.LessThan n) {
        n.e1.accept(this);
        Tree.Exp a = resultExp;
        n.e2.accept(this);
        Tree.Exp b = resultExp;
        Temp t = new Temp();
        Label lt = new Label();
        Label lf = new Label();
        Label le = new Label();

        Tree.Stm s = seq(
            new Tree.MOVE(new Tree.TEMP(t), const0()),
            new Tree.CJUMP(Tree.CJUMP.LT, a, b, lt, lf),
            new Tree.LABEL(lt),
            new Tree.MOVE(new Tree.TEMP(t), const1()),
            new Tree.JUMP(le),
            new Tree.LABEL(lf),
            new Tree.LABEL(le)
        );
        resultExp = new Tree.ESEQ(s, new Tree.TEMP(t));
    }

    public void visit(syntaxtree.Plus n) {
        n.e1.accept(this);
        Tree.Exp a = resultExp;
        n.e2.accept(this);
        Tree.Exp b = resultExp;
        resultExp = bin(Tree.BINOP.PLUS, a, b);
    }

    public void visit(syntaxtree.Minus n) {
        n.e1.accept(this);
        Tree.Exp a = resultExp;
        n.e2.accept(this);
        Tree.Exp b = resultExp;
        resultExp = bin(Tree.BINOP.MINUS, a, b);
    }

    public void visit(syntaxtree.Times n) {
        n.e1.accept(this);
        Tree.Exp a = resultExp;
        n.e2.accept(this);
        Tree.Exp b = resultExp;
        resultExp = bin(Tree.BINOP.MUL, a, b);
    }

    public void visit(syntaxtree.ArrayLookup n) {
        n.e1.accept(this);
        Tree.Exp base = resultExp;
        n.e2.accept(this);
        Tree.Exp idx = resultExp;
        // Data starts at base + 4
        Tree.Exp addr = bin(Tree.BINOP.PLUS, base, bin(Tree.BINOP.PLUS, new Tree.CONST(4), bin(Tree.BINOP.MUL, idx, new Tree.CONST(4))));
        resultExp = new Tree.MEM(addr);
    }

    public void visit(syntaxtree.ArrayLength n) {
        n.e.accept(this);
        // length is stored at base offset 0
        resultExp = new Tree.MEM(resultExp);
    }

    public void visit(syntaxtree.Call n) {
        String recvClass = receiverClassOf(n.e);
        n.e.accept(this);
        Tree.Exp recv = resultExp;

        Tree.ExpList args = new Tree.ExpList(recv, null);
        for (int i = 0; i < n.el.size(); i++) {
            n.el.elementAt(i).accept(this);
            args = append(args, resultExp);
        }

        String callName = (recvClass != null) ? methodLabel(recvClass, n.i.s) : n.i.s;
        resultExp = new Tree.CALL(new Tree.NAME(new Label(callName)), args);
    }

    public void visit(syntaxtree.IntegerLiteral n) {
        resultExp = new Tree.CONST(n.i);
    }

    public void visit(syntaxtree.True n) {
        resultExp = const1();
    }

    public void visit(syntaxtree.False n) {
        resultExp = const0();
    }

    public void visit(syntaxtree.IdentifierExp n) {
        resultExp = varExp(n.s);
    }

    public void visit(syntaxtree.This n) {
        resultExp = varExp("this");
    }

    public void visit(syntaxtree.NewArray n) {
        n.e.accept(this);
        Tree.Exp size = resultExp;
        Temp t = new Temp();
        Tree.Stm s = new Tree.MOVE(new Tree.TEMP(t), new Tree.CALL(new Tree.NAME(new Label(Mips.Frame.allocArrayLabel())), new Tree.ExpList(size, null)));
        resultExp = new Tree.ESEQ(s, new Tree.TEMP(t));
    }

    public void visit(syntaxtree.NewObject n) {
        String cname = n.i.s;
        int fcount = classFields.getOrDefault(cname, new ArrayList<>()).size();
        int sizeBytes = fcount * WORD_SIZE;
        Temp t = new Temp();
        List<Tree.Stm> inits = new ArrayList<>();

        inits.add(new Tree.MOVE(new Tree.TEMP(t), new Tree.CALL(new Tree.NAME(new Label(Mips.Frame.heapAllocLabel())), new Tree.ExpList(new Tree.CONST(sizeBytes), null))));

        for (int i = 0; i < fcount; i++) {
            int off = i * WORD_SIZE;
            inits.add(new Tree.MOVE(new Tree.MEM(bin(Tree.BINOP.PLUS, new Tree.TEMP(t), new Tree.CONST(off))), const0()));
        }

        resultExp = new Tree.ESEQ(seq(inits), new Tree.TEMP(t));
    }

    public void visit(syntaxtree.Not n) {
        n.e.accept(this);
        // Logical negation: XOR with 1
        resultExp = bin(Tree.BINOP.XOR, resultExp, const1());
    }

    public void visit(syntaxtree.Identifier n) {
        // Sub-elements handle identifiers directly by name string (.s)
    }
}
