package magic.firemind.ai;

import java.util.HashMap;

import prefuse.data.Graph;
import prefuse.data.Node;

public class TreeVisualizer {

    
    public Graph g = new Graph();
    public TreeVisualizer(){
        g.getNodeTable().addColumns(GameTreeView.LABEL_SCHEMA);
        g.getNodeTable().addColumns(GameTreeView.CHOICE_SCHEMA);
    }
    public HashMap<Integer, ChoiceNode> nodes = new HashMap<Integer, ChoiceNode>();
    public int choiceResultKey(Object[] choiceResults){
        int key = 0;
        for(Object o : choiceResults){
            if(o!= null)
                key+=o.hashCode();
        }
        return key;
    }

    public ChoiceNode addNode(ChoiceNode parent, String string) {
        Node child = g.addNode();
        
        ChoiceNode cn = new ChoiceNode(child, parent, string);
        nodes.put(null, cn);
        
        child.setString(GameTreeView.LABEL, cn.toLabel());
        if(parent != null){
            g.addEdge(parent.n, child);
        }
        return cn;
        
    }
    
    public synchronized ChoiceNode addNode(ChoiceNode parent, Object[] choiceResults){
        Node child = g.addNode();
        
        ChoiceNode cn = new ChoiceNode(child, parent, choiceResults);
        nodes.put(choiceResultKey(choiceResults), cn);
        
        child.setString(GameTreeView.LABEL, cn.toLabel());
        if(parent != null){
            g.addEdge(parent.n, child);
        }
        return cn;
    }
    
    public synchronized void colorNodePath(Object[] choiceResults){

        ChoiceNode cn = nodes.get(choiceResultKey(choiceResults));
        if(cn == null){
            System.out.println("Key not found "+choiceResultKey(choiceResults));
        }
        while(cn != null){
            Node n = cn.n;
            n.setBoolean(GameTreeView.CHOICE, true);
            cn = cn.parent;
        }
    }

    public void finish(Object[] choiceResults) {
        this.colorNodePath(choiceResults);
        GameTreeView.draw(g);
        this.nodes.clear();
    }
}
