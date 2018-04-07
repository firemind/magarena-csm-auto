package magic.ai;

import com.google.common.collect.Maps;
import magic.data.LRUCache;
import magic.exception.GameException;
import magic.firemind.CombatPredictionClient;
import magic.firemind.CombatScoreLog;
import magic.model.MagicGame;
import magic.model.MagicGameLog;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;
import magic.model.choice.MagicBuilderPayManaCostResult;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.event.MagicEvent;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/*
AI using Monte Carlo Tree Search

Classical MCTS (UCT)
 - use UCB1 formula for selection with C = sqrt(2)
 - reward either 0 or 1
 - backup by averaging
 - uniform random simulated playout
 - score = XX% (25000 matches against MMAB-1)

Enchancements to basic UCT
 - use ratio selection (v + 10)/(n + 10)
 - UCB1 with C = 1.0
 - UCB1 with C = 2.0
 - UCB1 with C = 3.0
 - use normal bound max(1,v + 2 * std(v))
 - reward depends on length of playout
 - backup by robust max

References:
UCT algorithm from Kocsis and Sezepesvari 2006

Consistency Modifications for Automatically Tuned Monte-Carlo Tree Search
  consistent -> child of root with greatest number of simulations is optimal
  frugal -> do not need to visit the whole tree
  eps-greedy is not consisteny for fixed eps (with prob eps select randomly, else use score)
  eps-greedy is consistent but not frugal if eps dynamically decreases to 0
  UCB1 is consistent but not frugal
  score = average is not consistent
  score = (total reward + K)/(total simulation + 2K) is consistent and frugal!
  using v_t threshold ensures consistency for case of reward in {0,1} using any score function
    v(s) < v_t (0.3), randomy pick a child, else pick child that maximize score

Monte-Carlo Tree Search in Lines of Action
  1-ply lookahread to detect direct win for player to move
  secure child formula for decision v + A/sqrt(n)
  evaluation cut-off: use score function to stop simulation early
  use evaluation score to remove "bad" moves during simulation
  use evaluation score to keep k-best moves
  mixed: start with corrective, rest of the moves use greedy
*/
public class GMCTSAI extends MagicAI {

    private static int MIN_SCORE = Integer.MAX_VALUE;
    static int MIN_SIM = Integer.MAX_VALUE;
    private static final int MAX_CHOICES = 1000;
    static double UCB1_C = 0.4;
    static double RATIO_K = 1.0;
    private int sims = 0;

    private CombatPredictionClient combatPredictionClient;
    static {
        if (System.getProperty("min_sim") != null) {
            MIN_SIM = Integer.parseInt(System.getProperty("min_sim"));
            System.err.println("MIN_SIM = " + MIN_SIM);
        }

        if (System.getProperty("min_score") != null) {
            MIN_SCORE = Integer.parseInt(System.getProperty("min_score"));
            System.err.println("MIN_SCORE = " + MIN_SCORE);
        }

        if (System.getProperty("ucb1_c") != null) {
            UCB1_C = Double.parseDouble(System.getProperty("ucb1_c"));
            System.err.println("UCB1_C = " + UCB1_C);
        }

        if (System.getProperty("ratio_k") != null) {
            RATIO_K = Double.parseDouble(System.getProperty("ratio_k"));
            System.err.println("RATIO_K = " + RATIO_K);
        }
    }

    private final boolean CHEAT;

    //cache nodes to reuse them in later decision
    private final LRUCache<Long, MCTSGameTree> CACHE = new LRUCache<Long, MCTSGameTree>(1000);

    public GMCTSAI(final boolean cheat) {
        CHEAT = cheat;
        combatPredictionClient = new CombatPredictionClient();
    }

    private void log(final String message) {
        MagicGameLog.log(message);
    }

    @Override
    public Object[] findNextEventChoiceResults(final MagicGame startGame, final MagicPlayer scorePlayer) {

        // Determine possible choices
        final MagicGame aiGame = new MagicGame(startGame, scorePlayer);
        if (!CHEAT) {
            aiGame.hideHiddenCards();
        }
        final MagicEvent event = aiGame.getNextEvent();
//        final List<Object[]> RCHOICES = orderedChoices(event.getArtificialChoiceResults(aiGame), aiGame );

        final List<Object[]> RCHOICES;
        List<Object[]> tmpChoices = event.getArtificialChoiceResults(aiGame);

        final int size = tmpChoices.size();

        // No choice
        assert size > 0 : "ERROR! No choice found at start of MCTS";

        // Single choice
        if (size == 1) {
            return startGame.map(tmpChoices.get(0));
        }

//        if(isCombatChoice(tmpChoices)) {
//            PriorityQueue<Map.Entry<Object[], Float>> q = prioritizedChoices(tmpChoices, aiGame);
//            if((-q.peek().getValue()) > 0.9) {
//                System.out.println(q.peek().getValue());
//                return startGame.map(q.peek().getKey());
//            }
////            System.out.println("Before"+ tmpChoices.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
//            RCHOICES = q.stream().map(Map.Entry::getKey).collect(Collectors.toList());
////            System.out.println("After scores "+ q.stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(", ")));
////            System.out.println("After "+ RCHOICES.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
//            assert RCHOICES.size() == size : "choice list size changed";
//        }else{
//            RCHOICES = tmpChoices;
//        }
        RCHOICES = orderedChoices(tmpChoices, aiGame);

        //root represents the start state
        final MCTSGameTree root = MCTSGameTree.getNode(CACHE, aiGame, RCHOICES);

        log("GMCTS cached=" + root.getNumSim());

        sims = 0;
        final ExecutorService executor = Executors.newFixedThreadPool(getMaxThreads());
//        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        // ensure tree update runs at least once
        final int aiLevel = scorePlayer.getAiProfile().getAiLevel();
        final long START_TIME = System.currentTimeMillis();
        final long END_TIME = START_TIME + 1000 * aiLevel;
        final Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                TreeUpdate(this, root, aiGame, executor, queue, END_TIME, RCHOICES);
            }
        };

        updateTask.run();

        try {
            // wait for artificialLevel + 1 seconds for jobs to finish
            executor.awaitTermination(aiLevel + 1, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            // force termination of workers
            executor.shutdownNow();
        }

        assert root.size() > 0 : "ERROR! Root has no children but there are " + size + " choices";

        //select the best child/choice
        final MCTSGameTree first = root.first();
        double maxD = first.getDecision();
        int bestC = first.getChoice();
        for (final MCTSGameTree node : root) {
            final double D = node.getDecision();
            final int C = node.getChoice();
            if (D > maxD) {
                maxD = D;
                bestC = C;
            }
        }

        log(outputChoice(scorePlayer, root, START_TIME, bestC, sims, RCHOICES));


        return startGame.map(RCHOICES.get(bestC));
    }

    private Runnable genSimulationTask(final MagicGame rootGame, final LinkedList<MCTSGameTree> path, final BlockingQueue<Runnable> queue) {
        return new Runnable() {
            @Override
            public void run() {
                // propagate result of random play up the path
                final double score = randomPlay(path.getLast(), rootGame);
                queue.offer(genBackpropagationTask(score, path));
            }
        };
    }

    private Runnable genBackpropagationTask(final double score, final LinkedList<MCTSGameTree> path) {
        return new Runnable() {
            @Override
            public void run() {
                final Iterator<MCTSGameTree> iter = path.descendingIterator();
                MCTSGameTree child = null;
                MCTSGameTree parent = null;
                while (iter.hasNext()) {
                    child = parent;
                    parent = iter.next();

                    parent.removeVirtualLoss();
                    parent.updateScore(child, score);
                }
            }
        };
    }

    public void TreeUpdate(
        final Runnable updateTask,
        final MCTSGameTree root,
        final MagicGame aiGame,
        final ExecutorService executor,
        final BlockingQueue<Runnable> queue,
        final long END_TIME,
        final List<Object[]> RCHOICES
    ) {

        //prioritize backpropagation tasks
        while (queue.isEmpty() == false) {
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                // occurs when shutdownNow is invoked
                return;
            }
        }

        sims++;

        //clone the MagicGame object for simulation
        final MagicGame rootGame = new MagicGame(aiGame, aiGame.getScorePlayer());

        //pass in a clone of the state,
        //genNewTreeNode grows the tree by one node
        //and returns the path from the root to the new node
        final LinkedList<MCTSGameTree> path = growTree(root, rootGame, RCHOICES);

        assert path.size() >= 2 : "ERROR! length of MCTS path is " + path.size();

        // play a simulated game to get score
        // update all nodes along the path from root to new node

        final boolean running = System.currentTimeMillis() < END_TIME;

        // submit random play to executor
        if (running) {
            try {
                executor.execute(genSimulationTask(rootGame, path, queue));
            } catch (RejectedExecutionException e) {
                // occurs when trying to submit to a execute that has shutdown
                return;
            }
        }

        // virtual loss + game theoretic value propagation
        final Iterator<MCTSGameTree> iter = path.descendingIterator();
        MCTSGameTree child = null;
        MCTSGameTree parent = null;
        while (iter.hasNext()) {
            child = parent;
            parent = iter.next();

            parent.recordVirtualLoss();

            if (child != null && child.isSolved()) {
                final int steps = child.getSteps() + 1;
                if (parent.isAI() && child.isAIWin()) {
                    parent.setAIWin(steps);
                } else if (parent.isOpp() && child.isAILose()) {
                    parent.setAILose(steps);
                } else if (parent.isAI() && child.isAILose()) {
                    parent.incLose(steps);
                } else if (parent.isOpp() && child.isAIWin()) {
                    parent.incLose(steps);
                }
            }
        }

        // end simulations once root is AI win or time is up
        if (running && root.isAIWin() == false) {
            try {
                executor.execute(updateTask);
            } catch (RejectedExecutionException e) {
                // occurs when trying to submit to a execute that has shutdown
                return;
            }
        } else {
            executor.shutdown();
        }
    }

    private String outputChoice(
        final MagicPlayer scorePlayer,
        final MCTSGameTree root,
        final long START_TIME,
        final int bestC,
        final int sims,
        final List<Object[]> RCHOICES
    ) {

        final StringBuilder out = new StringBuilder();
        final long duration = System.currentTimeMillis() - START_TIME;

        out.append("GMCTS" +
                   " cheat=" + CHEAT +
                   " index=" + scorePlayer.getIndex() +
                   " life=" + scorePlayer.getLife() +
                   " turn=" + scorePlayer.getGame().getTurn() +
                   " phase=" + scorePlayer.getGame().getPhase().getType() +
                   " sims=" + sims +
                   " time=" + duration);
        out.append('\n');

        for (final MCTSGameTree node : root) {
            if (node.getChoice() == bestC) {
                out.append("* ");
            } else {
                out.append("  ");
            }
            out.append('[');
            out.append((int)(node.getV() * 100));
            out.append('/');
            out.append(node.getNumSim());
            out.append('/');
            if (node.isAIWin()) {
                out.append("win");
                out.append(':');
                out.append(node.getSteps());
            } else if (node.isAILose()) {
                out.append("lose");
                out.append(':');
                out.append(node.getSteps());
            } else {
                out.append("?");
            }
            out.append(']');
            out.append(CR2String(RCHOICES.get(node.getChoice())));
            out.append('\n');
        }
        return out.toString().trim();
    }

    private LinkedList<MCTSGameTree> growTree(final MCTSGameTree root, final MagicGame game, final List<Object[]> RCHOICES) {
        final LinkedList<MCTSGameTree> path = new LinkedList<MCTSGameTree>();
        boolean found = false;
        MCTSGameTree curr = root;
        path.add(curr);

        for (List<Object[]> choices = getNextChoices(game, RCHOICES);
             !choices.isEmpty() && !Thread.currentThread().isInterrupted();
             choices = getNextChoices(game, RCHOICES)) {

            assert choices.size() > 0 : "ERROR! No choice at start of genNewTreeNode";

            assert !curr.hasDetails() || MCTSGameTree.checkNode(curr, choices) :
                "ERROR! Inconsistent node found" + "\n" +
                game + " " +
                printPath(path) + " " +
                MCTSGameTree.printNode(curr, choices);

            final MagicEvent event = game.getNextEvent();

            //first time considering the choices available at this node,
            //fill in additional details for curr
            if (!curr.hasDetails()) {
                curr.setIsAI(game.getScorePlayer() == event.getPlayer());
                curr.setMaxChildren(choices.size());
//                System.out.println("Setting "+ choices.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
                assert curr.setChoicesStr(choices);
            }

            //look for first non root AI node along this path and add it to cache
            if (!found && curr != root && curr.isAI()) {
                found = true;
//                assert curr.isCached() || printPath(path);
                MCTSGameTree.addNode(CACHE, game, curr);
            }

            //there are unexplored children of node
            //assume we explore children of a node in increasing order of the choices
            if (curr.size() < choices.size()) {
                final int idx = curr.size();
                final Object[] choice = choices.get(idx);
                final String choiceStr = MCTSGameTree.obj2String(choice[0]);
                game.executeNextEvent(choice);
                final MCTSGameTree child = new MCTSGameTree(curr, idx, game.getScore());
                assert (child.desc = choiceStr).equals(child.desc);
                curr.addChild(child);
                path.add(child);
                return path;

            //all the children are in the tree, find the "best" child to explore
            } else {

                assert curr.size() == choices.size() : "ERROR! Different number of choices in node and game" +
                    printPath(path) + MCTSGameTree.printNode(curr, choices);

                MCTSGameTree next = null;
                double bestS = Double.NEGATIVE_INFINITY ;
                for (final MCTSGameTree child : curr) {
                    final double raw = child.getUCT();
                    final double S = child.modify(raw);
                    if (S > bestS) {
                        bestS = S;
                        next = child;
                    }
                }

                //move down the tree
                curr = next;

                //update the game state and path
                try {

//                    if(choices.size() <= curr.getChoice()) {
//                        System.out.println("Choices " + choices.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
//                        System.out.println("Curr: " + curr.getChoice());
//                    }
                    game.executeNextEvent(choices.get(curr.getChoice()));
                } catch (final IndexOutOfBoundsException ex) {
                    printPath(path);
                    MCTSGameTree.printNode(curr, choices);
                    throw new GameException(ex, game);
                }
                path.add(curr);
            }
        }

        return path;
    }

    private List<Object[]> nextOrderedChoices(MagicGame game, List<Object[]> RCHOICES) {
        List<Object[]> ordered =  orderedChoices(getNextChoices(game, RCHOICES), game);
//        System.out.println("Ordered "+ ordered.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
        return ordered;
    }

    //returns a reward in the range [0, 1]
    private double randomPlay(final MCTSGameTree node, final MagicGame game) {
        //terminal node, no need for random play
        if (game.isFinished()) {
            if (game.getLosingPlayer() == game.getScorePlayer()) {
                node.setAILose(0);
                return 0.0;
            } else {
                node.setAIWin(0);
                return 1.0;
            }
        }

        if (!CHEAT) {
            game.showRandomizedHiddenCards();
        }
        final int[] counts = runSimulation(game);

        //System.err.println("COUNTS:\t" + counts[0] + "\t" + counts[1]);

        if (!game.isFinished()) {
            return 0.5;
        } else if (game.getLosingPlayer() == game.getScorePlayer()) {
            // bias losing simulations towards ones where opponent makes more choices
            return counts[1] / (2.0 * MAX_CHOICES);
        } else {
            // bias winning simulations towards ones where AI makes less choices
            return 1.0 - counts[0] / (2.0 * MAX_CHOICES);
        }
    }

    private boolean isCombatChoice(List<Object[]> choices){
        boolean isNonCombat=false;
        boolean isCombat=false;
        for(Object[] choice : choices){
          if(choice.length > 0 && choice[0] instanceof magic.model.choice.MagicDeclareAttackersResult){
              assert choice.length == 1 : "should only have one combat choice";
              isCombat=true;
          }else{
              isNonCombat=true;
          }
        }
        assert !(isCombat && isNonCombat) : "Got both combat and non combat choices";
        return isCombat;
    }

    private int[] runSimulation(final MagicGame game) {

        int aiChoices = 0;
        int oppChoices = 0;

        //use fast choices during simulation
        game.setFastChoices(true);

        // simulate game until it is finished or reached MAX_CHOICES
        while (aiChoices < MAX_CHOICES &&
               oppChoices < MAX_CHOICES &&
               !Thread.currentThread().isInterrupted() &&
               game.advanceToNextEventWithChoice()) {
            final MagicEvent event = game.getNextEvent();

            if (event.getPlayer() == game.getScorePlayer()) {
                aiChoices++;
            } else {
                oppChoices++;
            }

            //get simulation choice and execute
            final Object[] choice;
            Object[] bestCombatChoice= findBestCombatChoice(game, event.getArtificialChoiceResults(game), 0.5);
            if(bestCombatChoice == null){
               choice = event.getSimulationChoiceResult(game);
            }else{
               choice = bestCombatChoice;
//               System.out.println("combat choice: "+ Arrays.toString(bestCombatChoice));
            }
            assert choice != null : "ERROR! No choice found during MCTS sim";
            game.executeNextEvent(choice);

            //terminate early if score > MIN_SCORE or score < -MIN_SCORE
            if (game.getScore() < -MIN_SCORE) {
                game.setLosingPlayer(game.getScorePlayer());
            }
            if (game.getScore() > MIN_SCORE) {
                game.setLosingPlayer(game.getScorePlayer().getOpponent());
            }
        }

        //game is finished or reached MAX_CHOICES
        return new int[]{aiChoices, oppChoices};
    }

    private PriorityQueue<Map.Entry<Object[], Float>> prioritizedChoices(List<Object[]> choices, MagicGame game){
        PriorityQueue<Map.Entry<Object[], Float>> choiceQueue =
                new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
        List<CombatPredictionClient.CombatRep> combatReps = new ArrayList<>();
        MagicPlayer scorePlayer = game.getScorePlayer();
        MagicPlayer opp = game.getPlayers()[(scorePlayer.getIndex() + 1) % 2];
        List<Float> availableAttackersIds = combatPredictionClient.extractCardIds(scorePlayer.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::canAttack).
                            toArray());
        List<Float> blockersIds = combatPredictionClient.extractCardIds(opp.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::canBlock).
                            toArray());
        for(Object[] combatChoice : choices) {
            Object c = combatChoice[0];

            combatReps.add(combatPredictionClient.new CombatRep(
                    scorePlayer.getLife(),
                    opp.getLife(),
                    (MagicDeclareAttackersResult) c,
                    availableAttackersIds,
                    blockersIds
                    ));
        }
        int ix=0;
        for(Float score: combatPredictionClient.predictWin(combatReps)){
            // add small offset based on index to ensure consistent ordering
            choiceQueue.add(Maps.immutableEntry(choices.get(ix), -score+0.0000001f*ix));
            ix++;
        }
        return choiceQueue;
    }

    private List<Object[]> orderedChoices(List<Object[]> choices, MagicGame game) {
        if(choices.size() < 4 || !isCombatChoice(choices))
            return choices;
        PriorityQueue<Map.Entry<Object[], Float>> q =  prioritizedChoices(choices, game);
//            System.out.println("Ordered scores "+ q.stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(", ")));
        return q.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private Object[] findBestCombatChoice(MagicGame game, List<Object[]> choices, double scoreThreshold) {
        if(choices.size() < 3 || !isCombatChoice(choices))
            return null;
        Object[] bestCombatChoice=null;
        PriorityQueue<Map.Entry<Object[], Float>> queued = prioritizedChoices(choices, game);
//            System.out.println(queued.stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(", ")));
        Map.Entry<Object[], Float> choiceMap = queued.peek();
        if(-choiceMap.getValue() > scoreThreshold) {
            bestCombatChoice = choiceMap.getKey();
        }
        return bestCombatChoice;
    }

    private List<Object[]> getNextChoices(final MagicGame game, final List<Object[]> RCHOICES) {
        //disable fast choices
        game.setFastChoices(false);

        while (game.advanceToNextEventWithChoice()) {

            //do not accumulate score down the tree when not in simulation
            game.setScore(0);

            final MagicEvent event = game.getNextEvent();

            //get list of possible AI choices
            List<Object[]> choices = null;
            if (game.getNumActions() == 0) {
                //map the RCHOICES to the current game instead of recomputing the choices
                choices = new ArrayList<Object[]>(RCHOICES.size());
                for (final Object[] choice : RCHOICES) {
                    choices.add(game.map(choice));
                }
            } else {
                choices = orderedChoices(event.getArtificialChoiceResults(game), game);
            }
            assert choices != null;

            final int size = choices.size();
            assert size > 0 : "ERROR! No choice found during MCTS getACR";

            if (size == 1) {
                //single choice
                game.executeNextEvent(choices.get(0));
            } else {
                //multiple choice
                return choices;
            }
        }

        //game is finished
        return Collections.emptyList();
    }

    private static String CR2String(final Object[] choiceResults) {
        final StringBuilder buffer=new StringBuilder();
        if (choiceResults!=null) {
            buffer.append(" (");
            boolean first=true;
            for (final Object choiceResult : choiceResults) {
                if (first) {
                    first=false;
                } else {
                    buffer.append(',');
                }
                buffer.append(choiceResult);
            }
            buffer.append(')');
        }
        return buffer.toString();
    }

    private boolean printPath(final List<MCTSGameTree> path) {
        final StringBuilder sb = new StringBuilder();
        for (final MCTSGameTree p : path) {
            sb.append(" -> ").append(p.desc);
        }
        log(sb.toString());
        return true;
    }
}

