package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Plus extends Exp {
  public int line;
  public int column;
  public Exp e1,e2;
  
  public Plus(int l, int c, Exp ae1, Exp ae2) { 
    line = l; column = c;
    e1=ae1; e2=ae2;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
