import syntaxtree.*;
import visitor.*;

public class Main {
    public static void main(String [] args) {
        for (String path : args) {
            try {
                java.io.InputStream in = new java.io.FileInputStream(path);
                MiniJavaParser parser = new MiniJavaParser(in);
                Program root = parser.Goal();
                
                root.accept(new PrettyPrintVisitor()); 
                //System.out.println("Successfully parsed for " + path);


                TypeCheckVisitor typeChecker = new TypeCheckVisitor();
                typeChecker.setFileName(new java.io.File(path).getName());
                boolean success = typeChecker.check(root);
                

                if (success) {
                    System.out.println("Successfully type checked for " + path);
                    
                    // Chapter 7 IR Tree Translation
                    visitor.IRTranslator translator = new visitor.IRTranslator();
                    java.util.List<visitor.IRTranslator.ProcedureIR> procedures = translator.translate(root);
                    System.out.println("--- Chapter 7 IR Translation Result for " + path + " ---");
                    Tree.Print printIR = new Tree.Print(System.out);
                    for (visitor.IRTranslator.ProcedureIR proc : procedures) {
                        System.out.println("Procedure: " + proc.name);
                        printIR.prStm(proc.body);
                    }
                } else {
                    System.err.println("Type check failed for " + path);
                }
                
                if (path.endsWith("TestDuplicate.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 5) {
                        System.out.println("TEST PASS: TestDuplicate.java successfully generated exactly 5 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestDuplicate.java generated " + errorCount + " errors (expected 5).");
                    }
                } else if (path.endsWith("TestUndeclared.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 7) {
                        System.out.println("TEST PASS: TestUndeclared.java successfully generated exactly 7 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestUndeclared.java generated " + errorCount + " errors (expected 7).");
                    }
                } else if (path.endsWith("TestOverload.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 5) {
                        System.out.println("TEST PASS: TestOverload.java successfully generated exactly 5 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestOverload.java generated " + errorCount + " errors (expected 5).");
                    }
                } else if (path.endsWith("TestAcyclic.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 6) {
                        System.out.println("TEST PASS: TestAcyclic.java successfully generated exactly 6 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestAcyclic.java generated " + errorCount + " errors (expected 6).");
                    }
                } else if (path.endsWith("TestExprMismatch.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 14) {
                        System.out.println("TEST PASS: TestExprMismatch.java successfully generated exactly 14 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestExprMismatch.java generated " + errorCount + " errors (expected 14).");
                    }
                } else if (path.endsWith("TestStmtMismatch.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 7) {
                        System.out.println("TEST PASS: TestStmtMismatch.java successfully generated exactly 7 errors.");
                    } else {
                        System.err.println("TEST FAIL: TestStmtMismatch.java generated " + errorCount + " errors (expected 7).");
                    }
                }
            } catch (ParseException e) {
                System.err.println("Parse error in " + path + ":\n" + e.toString());
                System.exit(2);
            } catch (java.io.IOException e) {
                System.err.println("IO error: " + e.toString());
            }
        }
    }
}
