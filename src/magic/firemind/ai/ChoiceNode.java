package magic.firemind.ai;

import prefuse.data.Node;

public class ChoiceNode {
  public Node n;
  public ChoiceNode parent;
  public Object[] choiceResults;
  public String label = null;
  public ChoiceNode(Node n, ChoiceNode parent, Object[] choiceResults){
      this.n = n;
      this.parent = parent;

      String s = "";
      for (Object o: choiceResults){
          if(o != null)
              s+= o.toString();
      }
      this.label = s;
  }
  
  public ChoiceNode(Node n, ChoiceNode parent, String label){
      this.n = n;
      this.parent = parent;
      this.label = label;
  }
  
  public String toLabel(){
     return label;
  }
}
