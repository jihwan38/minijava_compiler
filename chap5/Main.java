import syntaxtree.*;
import visitor.*;

public class Main {
    public static void main(String [] args) {
        for (String path : args) {
            try {
                java.io.InputStream in = new java.io.FileInputStream(path);
                MiniJavaParser parser = new MiniJavaParser(in);
                Program root = parser.Goal();
                
                // root.accept(new PrettyPrintVisitor()); 
                System.out.println("Successfully parsed for " + path);


                // TODO: Symbol Table 및 TypeCheckVisitor 구현 후 아래 주석 해제
                /*
                boolean success = new TypeCheckVisitor().check(root);
                if (success) {
                    System.out.println("Successfully type checked for " + path);
                } else {
                    System.err.println("Type check failed for " + path);
                }
                */
            } catch (ParseException e) {
                System.err.println("Parse error in " + path + ":\n" + e.toString());
                System.exit(2);
            } catch (java.io.IOException e) {
                System.err.println("IO error: " + e.toString());
            }
        }
    }
}
