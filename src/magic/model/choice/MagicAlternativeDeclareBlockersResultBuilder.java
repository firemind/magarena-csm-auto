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
    private static final double LIMIT_THRESH = 100;
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
    private static final int[] nPartitions = new int[]{
            1, 1, 2, 3, 5, 7, 11, 15, 22, 30, 42, 56, 77, 101, 135, 176,
            231, 297, 385, 490, 627, 792, 1002, 1255, 1575, 1958, 2436,
            3010, 3718, 4565, 5604, 6842, 8349, 10143, 12310, 14883, 17977,
            21637, 26015, 31185, 37338, 44583, 53174, 63261, 75175, 89134,
            105558, 124754, 147273, 173525};

    MagicAlternativeDeclareBlockersResultBuilder(final MagicGame game,final MagicPlayer defendingPlayer,final boolean fast) {
        this.game=game;
        this.defendingPlayer=defendingPlayer;
        this.attackingPlayer=defendingPlayer.getOpponent();
        this.fast=fast;

        build();
    }

    Collection<Object> getResults() {
        return results == null ? EMPTY_RESULT : results;
    }

    private void buildBlockersFast() {
        System.err.println("Running alt randomized blocking algorithm");

        //sample NUM_SAMPLES random blocks
        final MagicRandom rng = new MagicRandom(attackers.size() + blockers.size());
        for (int i = 0; i < NUM_SAMPLES; i++) {
            MagicCombatCreature[] blockingSet = (MagicCombatCreature[]) blockers.keySet().toArray();
            MagicDeclareBlockersResult blockResult = new MagicDeclareBlockersResult(result, position++, 0);
            results.add(blockResult);
            rng.ints(1, blockers.size()).distinct().limit(rng.nextInt(blockers.size())).forEach(bIdx -> {
                MagicCombatCreature blocker = blockingSet[bIdx];
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
                          block = new MagicCombatCreature[blockers.keySet().size()+1];
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

        if (max_blocks > MIN_WARN) {
            System.err.println("WARNING. Number of blocking options is " + max_blocks);
        }

        if (max_blocks > RANDOM_THRESH) {
            buildBlockersFast();
        } else if (max_blocks > LIMIT_THRESH) {
            System.err.println("Building blockers for attacker");
            buildBlockersForAttacker(0);
        } else {
            System.err.println("Building all possible blocking combos");
            buildAllBlockerCombos();
        }
        if(results.isEmpty())
          System.err.println("Results is empty!");
    }

    private void buildAllBlockerCombos() {
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
        for(List<List<MagicCombatCreature>> block : blocks){
           MagicDeclareBlockersResult baseBlock = new MagicDeclareBlockersResult(result, position++, 0);
           for(List<MagicCombatCreature> b : block){
               baseBlock.add(b.toArray(new MagicCombatCreature[b.size()]));
           }
           results.add(baseBlock);
        }
    }

    private void attackerDupUnroll(List<MagicBlock> blocks, TreeMap<MagicCombatCreature,List<MagicCombatCreature>> attackers) {
    }

    private void mergeBlockingCombos(List<MagicBlock> perBlocker,List<MagicBlock> previousBlocks) {
        for(MagicBlock prev: new ArrayList<>(previousBlocks)){
            for(MagicBlock newBlocks: perBlocker){
                previousBlocks.add(prev.dup().merge(newBlocks));
            }
        }
    }

    private List<List<MagicCombatCreature>> mergeBlock(List<List<MagicCombatCreature>> newBlocks,List<List<MagicCombatCreature>> prev) {
        List<List<MagicCombatCreature>> mergedBlocks = new ArrayList<>();
        for(List<MagicCombatCreature> p : prev){
            mergedBlocks.add(new ArrayList<>(p));
        }
        for(List<MagicCombatCreature> newBlock : newBlocks) {
            List<MagicCombatCreature> mergedBlock = null;
            for(List<MagicCombatCreature> p : mergedBlocks) {
                if (prev.size() > 1 && p.get(0) == newBlock.get(0)) {
                   mergedBlock = p;
                   break;
                }
            }
            if(mergedBlock == null) {
                mergedBlock = new ArrayList<>();
                mergedBlock.add(newBlock.get(0));
                mergedBlocks.add(mergedBlock);
            }
            mergedBlock.addAll(newBlock.subList(1, newBlock.size()));

        }
        return mergedBlocks;
    }

    private void addAllBlocksForNumBlockers(List<MagicBlock> perBlocker, int numBlockDuplicates, MagicCombatCreature blocker) {
        List<MagicCombatCreature> blockerDups = blockers.get(blocker);
//        if(numBlockDuplicates > 1) {
//            System.err.println("helper(res, 1, " + numBlockDuplicates + ", new ArrayList<Integer>(), " + numBlockDuplicates+ ")");
//            System.err.println(partitions.stream().map(Object::toString)
//                    .collect(Collectors.joining(", ")));
//        }
        int totalBlockableAttackers = 0;
        ArrayList<MagicCombatCreature> attackPool = new ArrayList<>();
        for(MagicCombatCreature attacker : attackers.keySet()){
            if(Arrays.asList(attacker.candidateBlockers).contains(blocker)) {
                totalBlockableAttackers += 1;
                attackPool.add(attacker);
            }
        }
        for(MagicBlock.Partition partition: MagicBlock.getPartitions(numBlockDuplicates, 1, Math.min(totalBlockableAttackers, numBlockDuplicates))){
            List<MagicBlock> toAdd = makeBlockParts(blocker, attackPool, partition.grouped());
            perBlocker.addAll(toAdd);
        }
//                int maxBlockableAttackers = Math.min(attackerDups.size(), numBlockDuplicates);
//                List<List<Integer>> res = new ArrayList<List<Integer>>();
//                for(int numBlockableAttackers=1; numBlockableAttackers<=maxBlockableAttackers;numBlockableAttackers++){
//                    partitions(res, 1, numBlockDuplicates, new ArrayList<Integer>(), numBlockableAttackers);
//                    if(numBlockableAttackers > 1) {
//                        System.err.println("helper(res, 1, " + numBlockDuplicates + ", new ArrayList<Integer>(), " + numBlockableAttackers + ")");
//                        System.err.println(res.stream().map(Object::toString)
//                                .collect(Collectors.joining(", ")));
//                    }
//                }
//                for(List<Integer> combo: res){
//                    List<List<MagicCombatCreature>> b = new ArrayList<>();
//                    int i = 0;
//                    int offset=0;
//                    for(Integer n : combo){
//                        List<MagicCombatCreature> e = new ArrayList<>();
//                        e.add(attackerDups.get(i));
//                        e.addAll(blockerDups.subList(offset, offset+n));
//                        if(n > 1) {
//                            System.err.println(e.stream().map(MagicCombatCreature::getName)
//                                    .collect(Collectors.joining(", ")));
//                        }
//                        b.add(e);
//                        offset += n;
//                        i++;
//                    }
//                    perBlocker.add(b);
//                }
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
        dfs(list, n, k, 0, item, result); // because it need to begin from 1

        return result;
    }

    private void dfs(List<MagicCombatCreature> list, int n, int k, int start, List<MagicCombatCreature> item,
        List<List<MagicCombatCreature>> res) {
        if (item.size() == k) {
            res.add(new ArrayList<MagicCombatCreature>(item));
            return;
        }

        for (int i = start; i <= n; i++) {
            item.add(list.get(i));
            dfs(list, n, k, i + 1, item, res);
            item.remove(item.size() - 1);
        }
    }


}

//                    MagicDeclareBlockersResult baseBlock = new MagicDeclareBlockersResult(result, position++, 0);
//                    baseBlock.add(new MagicCombatCreature[]{attacker});
//                for(int maxb=1;maxb<=numBlockDuplicates;maxb++){
//                    for(int ai=0;ai<numAttackDuplicates;ai++) {
//                        for(Object res : new ArrayList<>(results)){
//                            MagicDeclareBlockersResult withDuplicate = new MagicDeclareBlockersResult((MagicDeclareBlockersResult) res, position++, 0);
//                            for(MagicCombatCreature attackerDup : attackers.get(attacker).stream().limit(ai+1).collect(Collectors.toList())){
//                                MagicCombatCreature[] block = null;
//                                for(MagicCombatCreature[] blockByAttacker : withDuplicate){
//                                    if(blockByAttacker[0] == attackerDup){
//                                        block = blockByAttacker;
//                                        break;
//                                    }
//                                }
//                                if(block == null) {
//                                    block = new MagicCombatCreature[maxb+1];
//                                    block[0] = attackerDup;
//                                    block[1] = blocker;
//                                    withDuplicate.add(block);
//                                }else {
//                                    block[maxb] = blockers.get(blocker).get(maxb - 1);
//                                }
//                            }
//                            results.add(withDuplicate);
//                        }
//                    }
//                }
    }


}
