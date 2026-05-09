package visitor;

import syntaxtree.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckVisitor implements TypeVisitor {

    private final HashMap<String, ClassInfo> classes = new HashMap<>();
    private ClassInfo currentClass = null;
    private MethodInfo currentMethod = null;
    public enum Phase { COLLECT, CHECK }
    public Phase phase = Phase.COLLECT;
    private final ArrayList<String> errors = new ArrayList<>();

    public List<String> getErrors() { return errors; }

    public boolean check(Program program) {
        phase = Phase.COLLECT;
        program.accept(this);
        
        phase = Phase.CHECK;
        program.accept(this);
        
        for (String e : errors) {
            System.err.println(e);
        }
        
        return errors.isEmpty();
    }

    private void error(String msg) {
        errors.add(msg);
    }

    public static class MethodInfo {
        String name;
        Type returnType;
        LinkedHashMap<String, Type> params = new LinkedHashMap<>();
        HashMap<String, Type> locals = new HashMap<>();

        public MethodInfo(String name, Type returnType) {
            this.name = name;
            this.returnType = returnType;
        }

        public boolean addParam(String id, Type t) {
            if (params.containsKey(id)) return false;
            params.put(id, t);
            return true;
        }

        public boolean addLocal(String id, Type t) {
            if (params.containsKey(id) || locals.containsKey(id)) return false;
            locals.put(id, t);
            return true;
        }

        public String getSignature() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");
            int i = 0;
            for (Type t : params.values()) {
                if (i > 0) sb.append(",");
                if (t instanceof IntegerType) sb.append("int");
                else if (t instanceof BooleanType) sb.append("boolean");
                else if (t instanceof IntArrayType) sb.append("int[]");
                else if (t instanceof IdentifierType) sb.append(((IdentifierType)t).s);
                i++;
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public static class ClassInfo {
        String name;
        String parent; 
        HashMap<String, Type> fields = new HashMap<>();
        HashMap<String, MethodInfo> methods = new HashMap<>();

        public boolean addVar(String id, Type t) {
            if (fields.containsKey(id)) return false;
            fields.put(id, t);
            return true;
        }

        public boolean addMethod(String id, MethodInfo m) {
            if (methods.containsKey(id)) return false;
            methods.put(id, m);
            return true;
        }
    }

    public boolean addClass(String id, String parent) {
        if (classes.containsKey(id)) return false;
        ClassInfo c = new ClassInfo();
        c.name = id;
        c.parent = parent;
        classes.put(id, c);
        return true;
    }

    private Type getVarType(String id) {
        if (currentMethod != null) {
            if (currentMethod.locals.containsKey(id)) return currentMethod.locals.get(id);
            if (currentMethod.params.containsKey(id)) return currentMethod.params.get(id);
        }
        
        ClassInfo c = currentClass;
        while (c != null) {
            if (c.fields.containsKey(id)) return c.fields.get(id);
            if (c.parent != null) {
                c = classes.get(c.parent);
            } else {
                c = null;
            }
        }
        return null;
    }

    public Type visit(Program n) { 
        n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) {
            n.cl.elementAt(i).accept(this);
        }
        return null; 
    }

    public Type visit(MainClass n) { 
        if (phase == Phase.COLLECT) {
            if (!addClass(n.i1.s, null)) {
                error("class " + n.i1.s + " is already defined");
            }
        } else {
            n.s.accept(this);
        }
        return null; 
    }

    public Type visit(ClassDeclSimple n) { 
        if (phase == Phase.COLLECT) {
            if (!addClass(n.i.s, null)) {
                error("class " + n.i.s + " is already defined");
            }
            currentClass = classes.get(n.i.s);
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.ml.size(); i++) {
                n.ml.elementAt(i).accept(this);
            }
            currentClass = null;
        } else {
            currentClass = classes.get(n.i.s);
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.ml.size(); i++) {
                n.ml.elementAt(i).accept(this);
            }
            currentClass = null;
        }
        return null; 
    }

    public Type visit(ClassDeclExtends n) { 
        if (phase == Phase.COLLECT) {
            if (!addClass(n.i.s, n.j.s)) {
                error("class " + n.i.s + " is already defined");
            }
            currentClass = classes.get(n.i.s);
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.ml.size(); i++) {
                n.ml.elementAt(i).accept(this);
            }
            currentClass = null;
        } else {
            if (!classes.containsKey(n.j.s)) {
                error("cannot find symbol: class " + n.j.s);
            }
            currentClass = classes.get(n.i.s);
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.ml.size(); i++) {
                n.ml.elementAt(i).accept(this);
            }
            currentClass = null;
        }
        return null; 
    }

    public Type visit(VarDecl n) { 
        if (phase == Phase.COLLECT) {
            if (currentMethod != null) {
                if (!currentMethod.addLocal(n.i.s, n.t)) {
                    error("variable " + n.i.s + " is already defined in method " + currentMethod.getSignature());
                }
            } else if (currentClass != null) {
                if (!currentClass.addVar(n.i.s, n.t)) {
                    error("variable " + n.i.s + " is already defined in class " + currentClass.name);
                }
            }
        } else {
            n.t.accept(this);
        }
        return null; 
    }

    public Type visit(MethodDecl n) { 
        if (phase == Phase.COLLECT) {
            MethodInfo m = new MethodInfo(n.i.s, n.t);
            if (!currentClass.addMethod(n.i.s, m)) {
                error("method " + n.i.s + " is already defined in class " + currentClass.name);
            } else {
                currentMethod = m;
                for (int i = 0; i < n.fl.size(); i++) {
                    n.fl.elementAt(i).accept(this);
                }
                for (int i = 0; i < n.vl.size(); i++) {
                    n.vl.elementAt(i).accept(this);
                }
                currentMethod = null;
            }
        } else {
            currentMethod = currentClass.methods.get(n.i.s);
            n.t.accept(this);
            for (int i = 0; i < n.fl.size(); i++) {
                n.fl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.sl.size(); i++) {
                n.sl.elementAt(i).accept(this);
            }
            n.e.accept(this);
            currentMethod = null;
        }
        return null; 
    }

    public Type visit(Formal n) { 
        if (phase == Phase.COLLECT) {
            if (!currentMethod.addParam(n.i.s, n.t)) {
                error("variable " + n.i.s + " is already defined in method " + currentMethod.getSignature());
            }
        } else {
            n.t.accept(this);
        }
        return null; 
    }
    public Type visit(IntArrayType n) { return null; }
    public Type visit(BooleanType n) { return null; }
    public Type visit(IntegerType n) { return null; }
    public Type visit(IdentifierType n) { 
        if (phase == Phase.CHECK) {
            if (!classes.containsKey(n.s)) {
                error("cannot find symbol: class " + n.s);
            }
        }
        return null; 
    }
    public Type visit(Block n) { 
        if (phase == Phase.CHECK) {
            for (int i = 0; i < n.sl.size(); i++) {
                n.sl.elementAt(i).accept(this);
            }
        }
        return null; 
    }
    public Type visit(If n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
            n.s1.accept(this);
            n.s2.accept(this);
        }
        return null; 
    }
    public Type visit(While n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
            n.s.accept(this);
        }
        return null; 
    }
    public Type visit(Print n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
        }
        return null; 
    }
    public Type visit(Assign n) { 
        if (phase == Phase.CHECK) {
            if (getVarType(n.i.s) == null) {
                error("cannot find symbol: variable " + n.i.s);
            }
            n.e.accept(this);
        }
        return null; 
    }
    public Type visit(ArrayAssign n) { 
        if (phase == Phase.CHECK) {
            if (getVarType(n.i.s) == null) {
                error("cannot find symbol: variable " + n.i.s);
            }
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(And n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(LessThan n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(Plus n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(Minus n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(Times n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(ArrayLookup n) { 
        if (phase == Phase.CHECK) {
            n.e1.accept(this);
            n.e2.accept(this);
        }
        return null; 
    }
    public Type visit(ArrayLength n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
        }
        return null; 
    }
    public Type visit(Call n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
            for (int i = 0; i < n.el.size(); i++) {
                n.el.elementAt(i).accept(this);
            }
        }
        return null; 
    }
    public Type visit(IntegerLiteral n) { return null; }
    public Type visit(True n) { return null; }
    public Type visit(False n) { return null; }
    public Type visit(IdentifierExp n) { 
        if (phase == Phase.CHECK) {
            if (getVarType(n.s) == null) {
                error("cannot find symbol: variable " + n.s);
            }
        }
        return null; 
    }
    public Type visit(This n) { return null; }
    public Type visit(NewArray n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
        }
        return null; 
    }
    public Type visit(NewObject n) { 
        if (phase == Phase.CHECK) {
            if (!classes.containsKey(n.i.s)) {
                error("cannot find symbol: class " + n.i.s);
            }
        }
        return null; 
    }
    public Type visit(Not n) { 
        if (phase == Phase.CHECK) {
            n.e.accept(this);
        }
        return null; 
    }
    public Type visit(Identifier n) { return null; }
}
