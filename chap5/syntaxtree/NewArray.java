package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class NewArray extends Exp {

  public Exp e;
  
  public NewArray(int l, int c, Exp ae) {
    line = l; column = c;
    e=ae; 
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
