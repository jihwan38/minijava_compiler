package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class If extends Statement {
  public int line;
  public int column;
  public Exp e;
  public Statement s1,s2;

  public If(int l, int c, Exp ae, Statement as1, Statement as2) {
    line = l; column = c;
    e=ae; s1=as1; s2=as2;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}

