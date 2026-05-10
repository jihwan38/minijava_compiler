package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class IdentifierType extends Type {

  public String s;

  public IdentifierType(int l, int c, String as) {
    line = l; column = c;
    s=as;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
