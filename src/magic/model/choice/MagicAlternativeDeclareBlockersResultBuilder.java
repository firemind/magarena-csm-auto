package magic.model.choice;

import com.google.common.collect.Sets;
import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;
import magic.model.MagicRandom;
import magic.model.score.MagicCombatScore;

import java.util.*;
import java.util.stream.Collectors;

public class MagicAlternativeDeclareBlockersResultBuilder {

    private static final Collection<Object> EMPTY_RESULT =
        Collections.<Object>singletonList(new MagicDeclareBlockersResult(0,0));
    private static final int MAX_RESULTS=12;
    private static final int MAX_ATTACKERS=3;
    private static final int MAX_TURN=1;
    private static final double MIN_WARN    = 1e5;
    private static final double RANDOM_THRESH = 1e5;
    private static final double LIMIT_THRESH = 75;
    private static final double DUP_THRESH = 0;
    private static final double NUM_SAMPLES = 1e4;

    private final MagicGame game;
    private final MagicPlayer attackingPlayer;
    private final MagicPlayer defendingPlayer;
    private final boolean fast;
    private Collection<Object> results = new ArrayList<>();
    private MagicDeclareBlockersResult result;
    private MagicCombatScore combatScore;
    private TreeMap<MagicCombatCreature, List<MagicCombatCreature>> attackers;
    private TreeMap<MagicCombatCreature, List<MagicCombatCreature>> blockers;
    private int position;

    MagicAlternativeDeclareBlockersResultBuilder(final MagicGame game,final MagicPlayer defendingPlayer,final boolean fast) {
        this.game=game;
        this.defendingPlayer=defendingPlayer;
        this.attackingPlayer=defendingPlayer.getOpponent();
        this.fast=fast;

        build();
    }

    Collection<Object> getResults() {
        return results.isEmpty() ? EMPTY_RESULT : results;
    }

    private void buildBlockersFast() {
//        System.err.println("Running alt randomized blocking algorithm");

        //sample NUM_SAMPLES random blocks
        final MagicRandom rng = new MagicRandom(attackers.size() + blockers.size());
        List<MagicCombatCreature> blockingSet = new ArrayList<>( blockers.keySet());
        for (int i = 0; i < NUM_SAMPLES; i++) {
            MagicDeclareBlockersResult blockResult = new MagicDeclareBlockersResult(result, position++, 0);
            results.add(blockResult);
            rng.ints(1, blockers.size()).distinct().limit(rng.nextInt(blockers.size())).forEach(bIdx -> {
                MagicCombatCreature blocker = blockingSet.get(bIdx);
                for(MagicCombatCreature attacker : attackers.keySet()) {
                    if (Arrays.asList(attacker.candidateBlockers).contains(blocker)) {
                        MagicCombatCreature[] block = null;
                        for(MagicCombatCreature[] blockByAttacker : blockResult){
                            if(blockByAttacker[0] == attacker){
                                block = blockByAttacker;
                                break;
                            }
                        }
                        if(block == null)
                          block = new MagicCombatCreature[blockers.size()+1];
                          block[0] = attacker;
                          block[1] = blocker;
                        block[bIdx+1] = blocker;
                    }
                }
            });
        }
    }

    private void buildBlockersForAttacker(final int index) {

        // A new result is found.
        if (index==attackers.size()) {
            results.add(new MagicDeclareBlockersResult(result,position++,0));
            return;
        }

        // Get the remaining candidate blockers.
        final MagicCombatCreature attacker= (MagicCombatCreature) attackers.keySet().toArray()[index];
        final MagicCombatCreature[] candidateBlockers=new MagicCombatCreature[attacker.candidateBlockers.length];
        int blockersSize=0;
        for (final MagicCombatCreature blocker : attacker.candidateBlockers) {
            if (blockers.keySet().contains(blocker)) {
                candidateBlockers[blockersSize++]=blocker;
            }
        }

        // No blockers.
        result.addLast(new MagicCombatCreature[]{attacker});
        buildBlockersForAttacker(index+1);
        result.removeLast();
        if (blockersSize == 0) {
            return;
        }

        // One blocker.
        if (blockersSize == 1) {
            final MagicCombatCreature blocker = candidateBlockers[0];
            List<MagicCombatCreature> dups = blockers.remove(blocker);
            result.addLast(new MagicCombatCreature[]{attacker,blocker});
            buildBlockersForAttacker(index+1);
            result.removeLast();
            blockers.put(blocker, dups);
            return;
        }

        // Single blocker which does not deal lethal damage to the attacker.
        // Not sufficient: might want to chump block with multiple blockers to
        // survive the attack or damage the attackers enough to finish it off
        // with direct damage
        int lethalDamage = attacker.lethalDamage;
        for (int blockerIndex = 0; blockerIndex < blockersSize; blockerIndex++) {
            final MagicCombatCreature blocker=candidateBlockers[blockerIndex];
            if (blocker.power < lethalDamage) {
                List<MagicCombatCreature> dups = blockers.remove(blocker);
                result.addLast(new MagicCombatCreature[]{attacker,blocker});
                buildBlockersForAttacker(index+1);
                result.removeLast();
                blockers.put(blocker, dups);
            }
        }

        // All combinations of blockers that deal lethal damage to the attacker.
        final MagicCombatCreature[] creatures = new MagicCombatCreature[blockersSize+1];
        creatures[0] = attacker;
        int size = 1;
        final int[] blockerSteps = new int[blockersSize];
        final int lastBlockerIndex = blockersSize-1;
        int blockerIndex = 0;
        MagicCombatCreature blocker;
        List<MagicCombatCreature> dups= null;
        while (blockerIndex >= 0) {
            switch (blockerSteps[blockerIndex]++) {
                case 0:
                    blocker = candidateBlockers[blockerIndex];
                    dups = blockers.remove(blocker);
                    lethalDamage -= blocker.power;
                    creatures[size++] = blocker;
                    // Lethal blocking combination.
                    if (lethalDamage <= 0) {
                        result.addLast(Arrays.copyOf(creatures,size));
                        buildBlockersForAttacker(index+1);
                        result.removeLast();
                    } else if (blockerIndex < lastBlockerIndex) {
                        blockerIndex++;
                    }
                    break;
                case 1:
                    blocker = candidateBlockers[blockerIndex];
                    blockers.put(blocker, dups);
                    lethalDamage += blocker.power;
                    size--;
                    if (blockerIndex < lastBlockerIndex) {
                        blockerIndex++;
                    }
                    break;
                case 2:
                    blockerSteps[blockerIndex--] = 0;
                    break;
            }
        }
    }

    private void build() {
        final MagicAlternativeCombatCreatureBuilder creatureBuilder=new MagicAlternativeCombatCreatureBuilder(game,attackingPlayer,defendingPlayer);

        // Check if none of the defending player's creatures can block.
        if (!creatureBuilder.buildBlockers()) {
            return;
        }

        // Check if none of the attackers can be blocked.
        blockers=creatureBuilder.getBlockers();
        if (!creatureBuilder.buildBlockableAttackers()) {
            return;
        }

        attackers= creatureBuilder.getAttackers();
        final boolean defending=game.getScorePlayer()==defendingPlayer;

        // number of blocking options is max_blocks
        double max_blocks = 1;
        for (final MagicPermanent blocker : creatureBuilder.getCandidateBlockers()) {
            max_blocks *= creatureBuilder.getBlockableAttackers(blocker).size();
        }

        // find best combinations of attackers and blockers.
        result=new MagicDeclareBlockersResult(0,0);
        position=0;

//        if (max_blocks > MIN_WARN) {
//            System.err.println("WARNING. Number of blocking options is " + max_blocks);
//        }

        if (max_blocks > RANDOM_THRESH) {
            buildBlockersFast();
        } else if (max_blocks > LIMIT_THRESH) {
//            if (countDups() > DUP_THRESH && max_blocks < 50){
//                buildAllBlockerCombosWithoutDups();
//            }else {
                buildBlockersForAttacker(0);
//            }
        } else {
            if (countDups() > 0) {
                buildAllBlockerCombosWithoutDups();
            } else {
                buildAllBlockerCombos();
            }
        }
        if(results.isEmpty())
          System.err.println("Results is empty!");
    }

    private int countDups(){
        int dups = 0;
        for(final MagicCombatCreature a: attackers.keySet())
            dups += (attackers.get(a).size()-1);
        for(final MagicCombatCreature b: blockers.keySet()) {
            if (blockers.get(b) != null) {
              dups += (blockers.get(b).size() - 1);
//              throw new RuntimeException("Invalid blocker config" + blockers.toString());
            }
        }
        return dups;
    }
    private void buildAllBlockerCombos() {
        List<MagicBlock> blocks = new ArrayList<>();
        blocks.add(new MagicBlock());
        for(MagicCombatCreature blocker:  blockers.keySet()){
            List<MagicBlock> perBlocker = new ArrayList<>();
            for(MagicCombatCreature attacker : attackers.keySet()){
                if(Arrays.asList(attacker.candidateBlockers).contains(blocker)) {
                    MagicBlock block = new MagicBlock();
                    block.addBlock(attacker, blocker, 1);
                    perBlocker.add(block);
                }
            }
            mergeBlockingCombos(perBlocker, blocks);
        }
        for(MagicBlock block: blocks)
            results.add(block.toDeclareBlockersResult());
    }

    private void buildAllBlockerCombosWithoutDups() {
        List<MagicBlock> blocks = new ArrayList<>();
        blocks.add(new MagicBlock());
        for(MagicCombatCreature blocker:  blockers.keySet()){
            int numBlockDuplicates = blockers.get(blocker).size();
            List<MagicBlock> perBlocker = new ArrayList<>();
            for(int i=1; i<=numBlockDuplicates;i++){
               addAllBlocksForNumBlockers(perBlocker, i, blocker);
            }
            mergeBlockingCombos(perBlocker, blocks);
        }
        attackerDupUnroll(blocks, attackers);

    }

    private void attackerDupUnroll(List<MagicBlock> blocks, TreeMap<MagicCombatCreature,List<MagicCombatCreature>> attackers) {
//        System.err.println("Unrolling blocks "+blocks.size());
        for(MagicBlock block: blocks){
            List<MagicBlock> newBlockVariants = new ArrayList<>();
            newBlockVariants.add(new MagicBlock());
            Map<MagicCombatCreature, Stack<MagicCombatCreature>> blockerMap = createBlockerMap();
//            System.err.println(block);
//            System.err.println(blockerMap.keySet());
//            System.err.println(blockerMap.values());
            for(MagicCombatCreature attacker : block.keySet()){
//                System.err.println("attacker: "+attacker.toString());
                List<MagicBlock> perAttackerVariants = new ArrayList<>();
                List<MagicCombatCreature> mappedBlockers = extractSpecificBlockers(block.get(attacker), blockerMap);
                int nAttackers = attackers.get(attacker).size();
                int nBlockers  = mappedBlockers.size();
                for(MagicBlock.Partition partition: MagicBlock.getPartitions(nBlockers, 1, Math.min(nBlockers, nAttackers))){
                    List<MagicBlock> toAdd = makeAttackParts(attacker, mappedBlockers, partition.grouped(), 0);
                    perAttackerVariants.addAll(toAdd);
                }
                newBlockVariants = newBlockVariants.stream().flatMap((variant) ->
                        perAttackerVariants.stream().map((ta) ->
                                ta.dup().merge(variant))
                ).collect(Collectors.toCollection(ArrayList::new));
            }
            for(MagicBlock finalBlock : newBlockVariants){
               results.add(finalBlock.toDeclareBlockersResult());
            }
//            System.err.println("results at "+results.size());
        }
    }

    private List<MagicBlock> makeAttackParts(MagicCombatCreature attacker, List<MagicCombatCreature> blockerPool, Stack<Map.Entry<Integer,List<Integer>>> groupedPartitions, int aix) {
        ArrayList<MagicCombatCreature> poolDup = new ArrayList<>(blockerPool);
        Map.Entry<Integer, List<Integer>> part = groupedPartitions.pop();
        List<MagicBlock> blocks = new ArrayList<>();
        for(List<MagicCombatCreature> bcombo : getCombinations(poolDup, part.getKey()*part.getValue().size())){
            int caix = aix;
            MagicBlock block = new MagicBlock();
            for(int bix=0; bix <part.getValue().size(); bix++){
                block.addBlocks(attackers.get(attacker).get(caix++), bcombo.subList(bix*part.getKey(), (bix+1)*part.getKey()));
            }
            if(groupedPartitions.empty()){
                blocks.add(block);
            }else{
                Stack<Map.Entry<Integer,List<Integer>>> dupStack = new Stack<>();
                dupStack.addAll(groupedPartitions);
                for(MagicBlock nextBlock : makeAttackParts(attacker, blockerPool.stream().filter(e -> !bcombo.remove(e)).collect(Collectors.toCollection(ArrayList::new)), dupStack, caix)){
                    blocks.add(nextBlock.merge(block));
                }
            }
        }
        return blocks;
    }

    private List<MagicCombatCreature> extractSpecificBlockers(List<Map.Entry<MagicCombatCreature,Integer>> entries, Map<MagicCombatCreature,Stack<MagicCombatCreature>> blockerMap) {
//        System.err.println(entries);
//        System.err.println(blockerMap);
        List<MagicCombatCreature> all = new ArrayList<>();
        for(Map.Entry<MagicCombatCreature,Integer> blocker : entries){
            Stack<MagicCombatCreature> list = blockerMap.get(blocker.getKey());
            for(int i=0;i<blocker.getValue();i++)
                if(list.isEmpty())
                    throw new RuntimeException("Requesting "+blocker.getKey()+" from empty list. others"+blockerMap);
                all.add(list.pop());
        }
        return all;
    }

    private Map<MagicCombatCreature, Stack<MagicCombatCreature>> createBlockerMap(){
        Map<MagicCombatCreature, Stack<MagicCombatCreature>> blockerMap = new TreeMap<>(MagicBlock.BLOCKER_COMPARATOR);
        for(MagicCombatCreature blocker : blockers.keySet()){
            Stack<MagicCombatCreature> stack = new Stack<MagicCombatCreature>();
            stack.addAll(blockers.get(blocker));
            blockerMap.put(blocker, stack);
        }
        return blockerMap;
    }

    private void mergeBlockingCombos(List<MagicBlock> perBlocker,List<MagicBlock> previousBlocks) {
        for(MagicBlock prev: new ArrayList<>(previousBlocks)){
            for(MagicBlock newBlocks: perBlocker){
                previousBlocks.add(prev.dup().merge(newBlocks));
            }
        }
    }


    private void addAllBlocksForNumBlockers(List<MagicBlock> perBlocker, int numBlockDuplicates, MagicCombatCreature blocker) {
        int totalBlockableAttackers = 0;
        ArrayList<MagicCombatCreature> attackPool = new ArrayList<>();
        for(MagicCombatCreature attacker : attackers.keySet()){
            if(Arrays.asList(attacker.candidateBlockers).contains(blocker)) {
                totalBlockableAttackers += 1;
                attackPool.add(attacker);
            }
        }
        if(totalBlockableAttackers == 0)
            return;
        for(MagicBlock.Partition partition: MagicBlock.getPartitions(numBlockDuplicates, 1, Math.min(totalBlockableAttackers, numBlockDuplicates))){
            List<MagicBlock> toAdd = makeBlockParts(blocker, attackPool, partition.grouped());
            perBlocker.addAll(toAdd);
        }
    }

    private List<MagicBlock> makeBlockParts(MagicCombatCreature blocker, ArrayList<MagicCombatCreature> attackPool, Stack<Map.Entry<Integer,List<Integer>>> groupedPartitions) {
        ArrayList<MagicCombatCreature> poolDup = new ArrayList<>(attackPool);
        Map.Entry<Integer, List<Integer>> part = groupedPartitions.pop();
        List<MagicBlock> blocks = new ArrayList<>();
        for(List<MagicCombatCreature> acombo : getCombinations(poolDup, part.getValue().size())){
            MagicBlock block = new MagicBlock();
            for(MagicCombatCreature a : acombo){
                block.addBlock(a, blocker, part.getKey());
            }
            if(groupedPartitions.empty()){
                blocks.add(block);
            }else{
                Stack<Map.Entry<Integer,List<Integer>>> dupStack = new Stack<>();
                dupStack.addAll(groupedPartitions);
                for(MagicBlock nextBlock : makeBlockParts(blocker, attackPool.stream().filter(e -> !acombo.remove(e)).collect(Collectors.toCollection(ArrayList::new)), dupStack)){
                    blocks.add(nextBlock.merge(block));
                }
            }
        }
        return blocks;
    }
    private List<List<MagicCombatCreature>> getCombinations(List<MagicCombatCreature> list, int k) {
        List<List<MagicCombatCreature>> result = new ArrayList<>();
        int n = list.size();

        if (n <= 0 || n < k)
            return result;

        ArrayList<MagicCombatCreature> item = new ArrayList<MagicCombatCreature>();
//        System.err.println("dfs(["+list.stream().map(MagicCombatCreature::getName).collect(Collectors.joining(", "))+"], "+n+", "+k+", 0, "+item+", result)");
        dfs(list, n, k, 0, item, result);

        return result;
    }

    private void dfs(List<MagicCombatCreature> list, int n, int k, int start, List<MagicCombatCreature> item,
        List<List<MagicCombatCreature>> res) {
        if (item.size() == k) {
            res.add(new ArrayList<MagicCombatCreature>(item));
            return;
        }

        for (int i = start; i < n; i++) {
            item.add(list.get(i));
            dfs(list, n, k, i + 1, item, res);
            item.remove(item.size() - 1);
        }
    }

}
