package visitor;

import syntaxtree.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class TypeCheckVisitor implements TypeVisitor {

    private HashMap<String, ClassInfo> classTable = new HashMap<>();
    private ClassInfo currClass = null;
    private MethodInfo currMethod = null;

    private static class ClassInfo {
        String parentName; 
        HashMap<String, Type> fields = new HashMap<>();
        HashMap<String, MethodInfo> methods = new HashMap<>();
    }
    private static class MethodInfo {
        Type returnType;
        LinkedHashMap<String, Type> params = new LinkedHashMap<>();
        HashMap<String, Type> locals = new HashMap<>();
    }
}
