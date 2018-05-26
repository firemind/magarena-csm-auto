package magic.model.choice;


import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;

public class MagicBlock extends HashMap<MagicCombatCreature, List<Entry<MagicCombatCreature,Integer>>> {
    public MagicBlock() {
        super();
    }
    public MagicBlock(MagicBlock magicBlock) {
        super(magicBlock);
    }

    void addBlock(MagicCombatCreature attacker, MagicCombatCreature blocker, Integer n){
        this.computeIfAbsent(attacker, (k)-> new ArrayList<>());
        List<Entry<MagicCombatCreature,Integer>> list = this.get(attacker);
        list.add(Maps.immutableEntry(blocker, n));
    }

    public MagicBlock dup(){
        return new MagicBlock(this);
    }

    public MagicBlock merge(MagicBlock other){
        for(MagicCombatCreature k : other.keySet()){
            this.computeIfAbsent(k, (v) -> new ArrayList());
            this.get(k).addAll(other.get(k));
        }
        return this;
    }

    public class Partition extends ArrayList<Integer>{
        public Partition(Partition other){
            super(other);
        }

        public Stack<Map.Entry<Integer, List<Integer>>> grouped(){
            Stack<Map.Entry<Integer, List<Integer>>> groups = new Stack<>();
            for(Integer i : this){
               Map.Entry<Integer, List<Integer>> list = groups.peek();
               if(list == null || !list.getKey().equals(i)){
                   list = Maps.immutableEntry(i, new ArrayList<>());
                   groups.push(list);
               }
               list.getValue().add(i);
            }
            return groups;
        }
    }

    public static List<Partition> getPartitions(int n, int from, int to){
        if(from > to)
            throw new InvalidParameterException("from is grater than to");
        List<Partition> partitions = new ArrayList<Partition>();
        for(int i=from; from <= to; from++)
            partitions(partitions, 1, n, new MagicBlock().new Partition(), i);
        return partitions;
    }

    private static void partitions(List<Partition> res, int start, int sum, Partition path, int k) {
        if(sum < 0) return;
        else if(sum == 0 && path.size() == k) res.add(path);
        else {
            for(int i = start; i <= 9; i++) {
                if(sum - i >= 0) {
                    Partition temp = new MagicBlock().new Partition(path);
                    temp.add(i);
                    partitions(res, i + 1, sum - i, temp, k);
                }
            }
        }
    }
}
