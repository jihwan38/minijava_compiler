package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;

public class MethodDecl {
  public int line;
  public int column;
  public Type t;
  public Identifier i;
  public FormalList fl;
  public VarDeclList vl;
  public StatementList sl;
  public Exp e;

  public MethodDecl(int l, int c, Type at, Identifier ai, FormalList afl, VarDeclList avl, 
                    StatementList asl, Exp ae) {
    line = l; column = c;
    t=at; i=ai; fl=afl; vl=avl; sl=asl; e=ae;
  }
 
  public void accept(Visitor v) {
    v.visit(this);
  }

  public Type accept(TypeVisitor v) {
    return v.visit(this);
  }
}
