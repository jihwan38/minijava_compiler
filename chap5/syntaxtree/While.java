package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class While extends Statement {

  public Exp e;
  public Statement s;

  public While(int l, int c, Exp ae, Statement as) {
    line = l; column = c;
    e=ae; s=as; 
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}

