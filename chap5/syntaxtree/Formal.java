package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Formal {
  public int line;
  public int column;
  public Type t;
  public Identifier i;
 
  public Formal(int l, int c, Type at, Identifier ai) {
    line = l; column = c;
    t=at; i=ai;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
