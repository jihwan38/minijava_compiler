package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Call extends Exp {
  public int line;
  public int column;
  public Exp e;
  public Identifier i;
  public ExpList el;
  
  public Call(int l, int c, Exp ae, Identifier ai, ExpList ael) {
    line = l; column = c;
    e=ae; i=ai; el=ael;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
