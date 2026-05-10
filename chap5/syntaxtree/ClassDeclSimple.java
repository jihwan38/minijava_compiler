package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class ClassDeclSimple extends ClassDecl {
  public int line;
  public int column;
  public Identifier i;
  public VarDeclList vl;  
  public MethodDeclList ml;
 
  public ClassDeclSimple(int l, int c, Identifier ai, VarDeclList avl, MethodDeclList aml) {
    line = l; column = c;
    i=ai; vl=avl; ml=aml;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
