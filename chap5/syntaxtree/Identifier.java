package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class Identifier {
  public int line;
  public int column;
  public String s;

  public Identifier(int l, int c, String as) { 
    line = l; column = c;
    s=as;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }

  public String toString(){
    return s;
  }
}
