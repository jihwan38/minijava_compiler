package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Program {
  public int line;
  public int column;
  public MainClass m;
  public ClassDeclList cl;

  public Program(int l, int c, MainClass am, ClassDeclList acl) {
    line = l; column = c;
    m=am; cl=acl; 
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
