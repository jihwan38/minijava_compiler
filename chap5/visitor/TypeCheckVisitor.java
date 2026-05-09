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
            currentClass = classes.get(n.i.s);
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
        }
        return null; 
    }
    public Type visit(IntArrayType n) { return null; }
    public Type visit(BooleanType n) { return null; }
    public Type visit(IntegerType n) { return null; }
    public Type visit(IdentifierType n) { return null; }
    public Type visit(Block n) { return null; }
    public Type visit(If n) { return null; }
    public Type visit(While n) { return null; }
    public Type visit(Print n) { return null; }
    public Type visit(Assign n) { return null; }
    public Type visit(ArrayAssign n) { return null; }
    public Type visit(And n) { return null; }
    public Type visit(LessThan n) { return null; }
    public Type visit(Plus n) { return null; }
    public Type visit(Minus n) { return null; }
    public Type visit(Times n) { return null; }
    public Type visit(ArrayLookup n) { return null; }
    public Type visit(ArrayLength n) { return null; }
    public Type visit(Call n) { return null; }
    public Type visit(IntegerLiteral n) { return null; }
    public Type visit(True n) { return null; }
    public Type visit(False n) { return null; }
    public Type visit(IdentifierExp n) { return null; }
    public Type visit(This n) { return null; }
    public Type visit(NewArray n) { return null; }
    public Type visit(NewObject n) { return null; }
    public Type visit(Not n) { return null; }
    public Type visit(Identifier n) { return null; }
}
