package magic.model.choice;


import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.Map.Entry;

public class MagicBlock extends TreeMap<MagicCombatCreature, List<Entry<MagicCombatCreature,Integer>>> {
    public static final Comparator<MagicCombatCreature> ATTACKER_COMPARATOR=new Comparator<MagicCombatCreature>() {
        @Override
        public int compare(final MagicCombatCreature attacker1,final MagicCombatCreature attacker2) {
            return Long.signum(attacker1.permanent.getStateId() - attacker2.permanent.getStateId());
//            return attacker1.permanent.compareTo(attacker2.permanent);
        }
    };
    public static final Comparator<MagicCombatCreature> BLOCKER_COMPARATOR=new Comparator<MagicCombatCreature>() {
        @Override
        public int compare(final MagicCombatCreature attacker1,final MagicCombatCreature attacker2) {
            return Long.signum(attacker1.permanent.getStateId() - attacker2.permanent.getStateId());
//            return attacker1.permanent.compareTo(attacker2.permanent);
        }
    };
    public MagicBlock() {
        super(ATTACKER_COMPARATOR);
    }
    public MagicBlock(MagicBlock magicBlock) {
        super(magicBlock);
    }

    void addBlock(MagicCombatCreature attacker, MagicCombatCreature blocker, Integer n){
        if(blocker == null)
            throw new RuntimeException("no blocker");
//        System.err.println("Adding blocker: "+attacker+" => "+blocker.toString());
        this.computeIfAbsent(attacker, (k)-> new ArrayList<>());
        List<Entry<MagicCombatCreature,Integer>> list = this.get(attacker);
        list.add(Maps.immutableEntry(blocker, n));
    }

    void addBlocks(MagicCombatCreature attacker, List<MagicCombatCreature> blockers){
        if(blockers.isEmpty())
            throw new RuntimeException("no blockers");
//        System.err.println("Adding blockers: "+blockers.toString());
        this.computeIfAbsent(attacker, (k)-> new ArrayList<>());
        List<Entry<MagicCombatCreature,Integer>> list = this.get(attacker);
        for(MagicCombatCreature blocker:blockers)
          list.add(Maps.immutableEntry(blocker, 1));
    }

    public MagicBlock dup(){
        MagicBlock newBlock = new MagicBlock();
        newBlock.merge(this);
        return newBlock;
    }

    public MagicBlock merge(MagicBlock other){
        for(MagicCombatCreature k : other.keySet()){
            this.computeIfAbsent(k, (v) -> new ArrayList());
            this.get(k).addAll(other.get(k));
        }
        return this;
    }

    public String toString(){

        String s="Block: ";
        for(MagicCombatCreature attacker : keySet()){
            s+= "\n* "+attacker.getName();
            s+= "\n  * "+get(attacker).toString();
        }
        return s;
    }

    public MagicDeclareBlockersResult toDeclareBlockersResult() {
        MagicDeclareBlockersResult result=new MagicDeclareBlockersResult(0,0);
        for(MagicCombatCreature attacker : keySet()){
            List<Entry<MagicCombatCreature,Integer>> blockers = get(attacker);
            if(blockers.isEmpty())
                throw new RuntimeException("no blockers");
            MagicCombatCreature[] b = new MagicCombatCreature[blockers.size()+1];
            b[0] = attacker;
            for(int i=1; i < b.length; i++ )
                b[i] = blockers.get(i-1).getKey();
            result.add(b);
        }
        return result;
    }

    public class Partition extends ArrayList<Integer>{
        public Partition(){
            super();
        }
        public Partition(Partition other){
            super(other);
        }

        public Stack<Map.Entry<Integer, List<Integer>>> grouped(){
            Stack<Map.Entry<Integer, List<Integer>>> groups = new Stack<>();
            for(Integer i : this){
               Map.Entry<Integer, List<Integer>> list = null;
               if(!groups.empty())
                   list = groups.peek();
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
