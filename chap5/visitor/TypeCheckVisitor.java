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
        Type returnType;
        LinkedHashMap<String, Type> params = new LinkedHashMap<>();
        HashMap<String, Type> locals = new HashMap<>();

        public MethodInfo(Type returnType) {
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

    public Type visit(Program n) { return null; }
    public Type visit(MainClass n) { return null; }
    public Type visit(ClassDeclSimple n) { return null; }
    public Type visit(ClassDeclExtends n) { return null; }
    public Type visit(VarDecl n) { return null; }
    public Type visit(MethodDecl n) { return null; }
    public Type visit(Formal n) { return null; }
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
