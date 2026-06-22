package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public abstract class ClassDecl {
  public int line;
  public int column;
  public abstract void accept(Visitor v);
  public abstract Type accept(TypeVisitor v);
}
