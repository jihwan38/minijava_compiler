package syntaxtree;

import java.util.Vector;

public class FormalList {
  public int line;
  public int column;
   private Vector list;

   public FormalList() {
    
      list = new Vector();
   }

   public void addElement(Formal n) {
      list.addElement(n);
   }

   public Formal elementAt(int i)  { 
      return (Formal)list.elementAt(i); 
   }

   public int size() { 
      return list.size(); 
   }
}
