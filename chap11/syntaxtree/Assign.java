package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Assign extends Statement {

  public Identifier i;
  public Exp e;

  public Assign(int l, int c, Identifier ai, Exp ae) {
    line = l; column = c;
    i=ai; e=ae; 
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}

