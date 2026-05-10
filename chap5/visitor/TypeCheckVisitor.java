package visitor;

import syntaxtree.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;

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

    private boolean isSameType(Type t1, Type t2) {
        if (t1 == null || t2 == null) return false;
        if (t1 instanceof IntegerType && t2 instanceof IntegerType) return true;
        if (t1 instanceof BooleanType && t2 instanceof BooleanType) return true;
        if (t1 instanceof IntArrayType && t2 instanceof IntArrayType) return true;
        if (t1 instanceof IdentifierType && t2 instanceof IdentifierType) {
            return ((IdentifierType) t1).s.equals(((IdentifierType) t2).s);
        }
        return false;
    }

    private boolean isSubtype(Type child, Type parent) {
        if (child == null || parent == null) return false;
        if (child instanceof IntegerType && parent instanceof IntegerType) return true;
        if (child instanceof BooleanType && parent instanceof BooleanType) return true;
        if (child instanceof IntArrayType && parent instanceof IntArrayType) return true;
        if (child instanceof IdentifierType && parent instanceof IdentifierType) {
            String cName = ((IdentifierType)child).s;
            String pName = ((IdentifierType)parent).s;
            while (cName != null) {
                if (cName.equals(pName)) return true;
                ClassInfo cInfo = classes.get(cName);
                if (cInfo == null) break;
                cName = cInfo.parent;
            }
        }
        return false;
    }

    private void checkOverloading(ClassInfo c, MethodInfo m) {
        if (c.parent == null) return;
        
        ClassInfo parentClass = classes.get(c.parent);
        while (parentClass != null) {
            if (parentClass.methods.containsKey(m.name)) {
                MethodInfo parentMethod = parentClass.methods.get(m.name);
                
                boolean isMatch = true;
                if (!isSameType(m.returnType, parentMethod.returnType)) isMatch = false;
                if (m.params.size() != parentMethod.params.size()) isMatch = false;
                else {
                    Iterator<Type> mParams = m.params.values().iterator();
                    Iterator<Type> pParams = parentMethod.params.values().iterator();
                    while (mParams.hasNext()) {
                        if (!isSameType(mParams.next(), pParams.next())) {
                            isMatch = false;
                            break;
                        }
                    }
                }
                
                if (!isMatch) {
                    error("method " + m.name + " in class " + c.name + " illegally overloads method in class " + parentClass.name);
                }
                return;
            }
            if (parentClass.parent != null) {
                parentClass = classes.get(parentClass.parent);
            } else {
                parentClass = null;
            }
        }
    }

    private void checkAcyclic(ClassInfo c) {
        if (c.parent == null) return;
        HashSet<String> visited = new HashSet<>();
        visited.add(c.name);
        
        String currentParent = c.parent;
        while (currentParent != null) {
            if (visited.contains(currentParent)) {
                error("cyclic inheritance involving class " + c.name);
                return;
            }
            visited.add(currentParent);
            ClassInfo parentInfo = classes.get(currentParent);
            if (parentInfo == null) break;
            currentParent = parentInfo.parent;
        }
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

    private MethodInfo getMethod(String className, String methodName) {
        ClassInfo curr = classes.get(className);
        while (curr != null) {
            if (curr.methods.containsKey(methodName)) return curr.methods.get(methodName);
            if (curr.parent != null) curr = classes.get(curr.parent);
            else curr = null;
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
            checkAcyclic(currentClass);
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
            }
            currentMethod = m;
            n.t.accept(this);
            for (int i = 0; i < n.fl.size(); i++) {
                n.fl.elementAt(i).accept(this);
            }
            for (int i = 0; i < n.vl.size(); i++) {
                n.vl.elementAt(i).accept(this);
            }
            currentMethod = null;
        } else {
            currentMethod = currentClass.methods.get(n.i.s);
            checkOverloading(currentClass, currentMethod);
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
            Type retType = n.e.accept(this);
            if (retType != null && !isSubtype(retType, n.t)) {
                error("incompatible types");
            }
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
            Type cond = n.e.accept(this);
            if (cond != null && !(cond instanceof BooleanType)) error("incompatible types");
            n.s1.accept(this);
            n.s2.accept(this);
        }
        return null; 
    }
    public Type visit(While n) { 
        if (phase == Phase.CHECK) {
            Type cond = n.e.accept(this);
            if (cond != null && !(cond instanceof BooleanType)) error("incompatible types");
            n.s.accept(this);
        }
        return null; 
    }
    public Type visit(Print n) { 
        if (phase == Phase.CHECK) {
            Type t = n.e.accept(this);
            if (t != null && !(t instanceof IntegerType)) error("incompatible types");
        }
        return null; 
    }
    public Type visit(Assign n) { 
        if (phase == Phase.CHECK) {
            Type varType = getVarType(n.i.s);
            if (varType == null) {
                error("cannot find symbol: variable " + n.i.s);
            }
            Type expType = n.e.accept(this);
            if (varType != null && expType != null && !isSubtype(expType, varType)) {
                error("incompatible types");
            }
        }
        return null; 
    }
    public Type visit(ArrayAssign n) { 
        if (phase == Phase.CHECK) {
            Type varType = getVarType(n.i.s);
            if (varType == null) {
                error("cannot find symbol: variable " + n.i.s);
            } else if (!(varType instanceof IntArrayType)) {
                error("array required, but non-array found");
            }
            Type indexType = n.e1.accept(this);
            if (indexType != null && !(indexType instanceof IntegerType)) error("incompatible types");
            Type valueType = n.e2.accept(this);
            if (valueType != null && !(valueType instanceof IntegerType)) error("incompatible types");
        }
        return null; 
    }
    public Type visit(And n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof BooleanType)) error("bad operand types for binary operator '&&'");
            if (t2 != null && !(t2 instanceof BooleanType)) error("bad operand types for binary operator '&&'");
        }
        return new BooleanType(); 
    }
    public Type visit(LessThan n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof IntegerType)) error("bad operand types for binary operator '<'");
            if (t2 != null && !(t2 instanceof IntegerType)) error("bad operand types for binary operator '<'");
        }
        return new BooleanType(); 
    }
    public Type visit(Plus n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof IntegerType)) error("bad operand types for binary operator '+'");
            if (t2 != null && !(t2 instanceof IntegerType)) error("bad operand types for binary operator '+'");
        }
        return new IntegerType(); 
    }
    public Type visit(Minus n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof IntegerType)) error("bad operand types for binary operator '-'");
            if (t2 != null && !(t2 instanceof IntegerType)) error("bad operand types for binary operator '-'");
        }
        return new IntegerType(); 
    }
    public Type visit(Times n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof IntegerType)) error("bad operand types for binary operator '*'");
            if (t2 != null && !(t2 instanceof IntegerType)) error("bad operand types for binary operator '*'");
        }
        return new IntegerType(); 
    }
    public Type visit(ArrayLookup n) { 
        if (phase == Phase.CHECK) {
            Type t1 = n.e1.accept(this);
            Type t2 = n.e2.accept(this);
            if (t1 != null && !(t1 instanceof IntArrayType)) error("array required, but non-array found");
            if (t2 != null && !(t2 instanceof IntegerType)) error("incompatible types");
        }
        return new IntegerType(); 
    }
    public Type visit(ArrayLength n) { 
        if (phase == Phase.CHECK) {
            Type t = n.e.accept(this);
            if (t != null && !(t instanceof IntArrayType)) error("array required, but non-array found");
        }
        return new IntegerType(); 
    }
    public Type visit(Call n) { 
        if (phase == Phase.CHECK) {
            Type objType = n.e.accept(this);
            if (objType == null) return null; 
            if (!(objType instanceof IdentifierType)) {
                error("cannot be dereferenced");
                return null;
            }
            String className = ((IdentifierType)objType).s;
            MethodInfo mInfo = getMethod(className, n.i.s);

            if (mInfo == null) {
                error("cannot find symbol: method " + n.i.s);
                return null;
            }

            if (n.el.size() != mInfo.params.size()) {
                error("actual and formal argument lists differ in length");
            } else {
                int i = 0;
                for (Type paramType : mInfo.params.values()) {
                    Type argType = n.el.elementAt(i).accept(this);
                    if (argType != null && !isSubtype(argType, paramType)) {
                        error("incompatible types");
                    }
                    i++;
                }
            }
            return mInfo.returnType;
        }
        return null; 
    }
    public Type visit(IntegerLiteral n) { return new IntegerType(); }
    public Type visit(True n) { return new BooleanType(); }
    public Type visit(False n) { return new BooleanType(); }
    public Type visit(IdentifierExp n) { 
        if (phase == Phase.CHECK) {
            Type t = getVarType(n.s);
            if (t == null) {
                error("cannot find symbol: variable " + n.s);
            }
            return t;
        }
        return null; 
    }
    public Type visit(This n) { 
        if (phase == Phase.CHECK) {
            if (currentClass != null) return new IdentifierType(currentClass.name);
        }
        return null; 
    }
    public Type visit(NewArray n) { 
        if (phase == Phase.CHECK) {
            Type t = n.e.accept(this);
            if (t != null && !(t instanceof IntegerType)) error("incompatible types");
        }
        return new IntArrayType(); 
    }
    public Type visit(NewObject n) { 
        if (phase == Phase.CHECK) {
            if (!classes.containsKey(n.i.s)) {
                error("cannot find symbol: class " + n.i.s);
                return null;
            }
            return new IdentifierType(n.i.s);
        }
        return null; 
    }
    public Type visit(Not n) { 
        if (phase == Phase.CHECK) {
            Type t = n.e.accept(this);
            if (t != null && !(t instanceof BooleanType)) error("bad operand type for unary operator '!'");
        }
        return new BooleanType(); 
    }
    public Type visit(Identifier n) { return null; }
}
