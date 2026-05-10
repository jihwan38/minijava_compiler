package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class NewObject extends Exp {
  public int line;
  public int column;
  public Identifier i;
  
  public NewObject(int l, int c, Identifier ai) {
    line = l; column = c;
    i=ai;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
