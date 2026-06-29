import syntaxtree.*;
import visitor.*;

public class Main {
    public static void main(String [] args) {
        for (String path : args) {
            try {
                java.io.InputStream in = new java.io.FileInputStream(path);
                MiniJavaParser parser = new MiniJavaParser(in);
                Program root = parser.Goal();
                
                // PrettyPrintVisitor is silenced to avoid polluting assembly output
                // root.accept(new PrettyPrintVisitor()); 

                TypeCheckVisitor typeChecker = new TypeCheckVisitor();
                typeChecker.setFileName(new java.io.File(path).getName());
                boolean success = typeChecker.check(root);
                

                if (success) {
                    System.err.println("# Successfully type checked for " + path);
                    
                    // Chapter 7 IR Tree Translation
                    visitor.IRTranslator translator = new visitor.IRTranslator();
                    java.util.List<visitor.IRTranslator.ProcedureIR> procedures = translator.translate(root);
                    
                    // Chapter 8 Canon, Chapter 9 Codegen, Chapter 10 & 11 RegAlloc
                    for (visitor.IRTranslator.ProcedureIR proc : procedures) {
                        Tree.StmList linearized = Canon.Canon.linearize(proc.body);
                        Canon.BasicBlocks blocks = new Canon.BasicBlocks(linearized);
                        Canon.TraceSchedule trace = new Canon.TraceSchedule(blocks);
                        
                        Codegen.Codegen codegen = new Codegen.Codegen(proc.frame);
                        Assem.InstrList instrs = codegen.codegen(trace.stms);
                        
                        // Chapter 10 procEntryExit2: Add liveness sink
                        instrs = proc.frame.procEntryExit2(instrs);
                        
                        // Chapter 11 Register Allocation
                        RegAlloc.RegAlloc regAlloc = new RegAlloc.RegAlloc(proc.frame, instrs);
                        Assem.InstrList allocatedBody = regAlloc.instrs;
                        
                        // Chapter 11 procEntryExit3: Prologue / Epilogue wrapping
                        Mips.Frame.Proc procOutput = proc.frame.procEntryExit3(allocatedBody);
                        
                        System.out.println(procOutput.prolog);
                        for (Assem.InstrList il = procOutput.body; il != null; il = il.tail) {
                            // Eliminate redundant moves (move $s0, $s0)
                            if (il.head instanceof Assem.MOVE) {
                                Assem.MOVE m = (Assem.MOVE) il.head;
                                String dstName = regAlloc.tempMap(m.dst);
                                String srcName = regAlloc.tempMap(m.src);
                                if (dstName != null && dstName.equals(srcName)) {
                                    continue;
                                }
                            }
                            System.out.print(il.head.format(regAlloc));
                        }
                        System.out.println(procOutput.epilog);
                    }
                    
                    // Print Runtime Library helper functions at the end
                    System.out.println(Mips.Frame.standard_library());
                } else {
                    System.err.println("Type check failed for " + path);
                }
                
                if (path.endsWith("TestDuplicate.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 5) {
                        System.err.println("# TEST PASS: TestDuplicate.java successfully generated exactly 5 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestDuplicate.java generated " + errorCount + " errors (expected 5).");
                    }
                } else if (path.endsWith("TestUndeclared.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 7) {
                        System.err.println("# TEST PASS: TestUndeclared.java successfully generated exactly 7 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestUndeclared.java generated " + errorCount + " errors (expected 7).");
                    }
                } else if (path.endsWith("TestOverload.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 5) {
                        System.err.println("# TEST PASS: TestOverload.java successfully generated exactly 5 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestOverload.java generated " + errorCount + " errors (expected 5).");
                    }
                } else if (path.endsWith("TestAcyclic.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 6) {
                        System.err.println("# TEST PASS: TestAcyclic.java successfully generated exactly 6 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestAcyclic.java generated " + errorCount + " errors (expected 6).");
                    }
                } else if (path.endsWith("TestExprMismatch.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 14) {
                        System.err.println("# TEST PASS: TestExprMismatch.java successfully generated exactly 14 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestExprMismatch.java generated " + errorCount + " errors (expected 14).");
                    }
                } else if (path.endsWith("TestStmtMismatch.java")) {
                    int errorCount = typeChecker.getErrors().size();
                    if (errorCount == 7) {
                        System.err.println("# TEST PASS: TestStmtMismatch.java successfully generated exactly 7 errors.");
                    } else {
                        System.err.println("# TEST FAIL: TestStmtMismatch.java generated " + errorCount + " errors (expected 7).");
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

