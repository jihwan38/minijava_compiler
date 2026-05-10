import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class PatchVisitor {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("visitor/TypeCheckVisitor.java")));
        
        // Find all public Type visit(NodeType n) or public void visit(NodeType n)
        // and replace error( inside them with error(n.line, n.column,
        
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("(public (?:Type|void) visit\\([a-zA-Z]+ n\\) \\{)(.*?)(^    \\})", Pattern.DOTALL | Pattern.MULTILINE).matcher(content);
        
        while (m.find()) {
            String decl = m.group(1);
            String body = m.group(2);
            String end = m.group(3);
            
            body = body.replace("error(", "error(n.line, n.column, ");
            
            m.appendReplacement(sb, Matcher.quoteReplacement(decl + body + end));
        }
        m.appendTail(sb);
        
        Files.write(Paths.get("visitor/TypeCheckVisitor.java"), sb.toString().getBytes());
        System.out.println("Patched TypeCheckVisitor!");
    }
}
