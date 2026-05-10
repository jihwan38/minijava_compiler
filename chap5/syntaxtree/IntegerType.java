package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class IntegerType extends Type {
  public int line;
  public int column;
  public IntegerType(int l, int c) { line = l; column = c; }
  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
