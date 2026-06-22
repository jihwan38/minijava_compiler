package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class MainClass {
  public int line;
  public int column;
  public Identifier i1,i2;
  public Statement s;

  public MainClass(int l, int c, Identifier ai1, Identifier ai2, Statement as) {
    line = l; column = c;
    i1=ai1; i2=ai2; s=as;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}

