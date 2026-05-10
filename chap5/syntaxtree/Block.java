package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Block extends Statement {
  public int line;
  public int column;
  public StatementList sl;

  public Block(int l, int c, StatementList asl) {
    line = l; column = c;
    sl=asl;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}

