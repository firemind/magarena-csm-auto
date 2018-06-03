package magic.ai;

import java.security.InvalidParameterException;
import com.google.common.collect.Maps;
import magic.data.LRUCache;
import magic.exception.GameException;
import magic.firemind.CombatPredictionClient;
import magic.firemind.CombatScoreLog;
import magic.model.*;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.choice.MagicDeclareBlockersResult;
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
    public static final String version = "0.1";

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
    private final boolean LOGCOMBAT;
    private final boolean ALTCHOICES;

    //cache nodes to reuse them in later decision
    private final LRUCache<Long, GMCTSGameTree> CACHE = new LRUCache<Long, GMCTSGameTree>(1000);

    public GMCTSAI(final boolean cheat, final boolean logcombat, final boolean altchoices) {
        CHEAT = cheat;
        combatPredictionClient = new CombatPredictionClient();
        LOGCOMBAT = logcombat;
        ALTCHOICES= altchoices;
    }

    public String getId(){
        return "GMCTS-"+version;
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
        if(ALTCHOICES) {
            RCHOICES = event.getAlternativeArtificialChoiceResults(aiGame);
        }else {
            RCHOICES = event.getArtificialChoiceResults(aiGame);
        }

        final int size = RCHOICES.size();

        // No choice
        assert size > 0 : "ERROR! No choice found at start of MCTS";

        // Single choice
        if (size == 1) {
            return startGame.map(RCHOICES.get(0));
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
//        RCHOICES = orderedChoices(tmpChoices, aiGame);
        //root represents the start state
        final GMCTSGameTree root = GMCTSGameTree.getNode(CACHE, aiGame, RCHOICES);

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
        final GMCTSGameTree first = root.first();
        double maxD = first.getDecision();
        int bestC = first.getChoice();
        for (final GMCTSGameTree node : root) {
            final double D = node.getDecision();
            final int C = node.getChoice();
            if (D > maxD) {
                maxD = D;
                bestC = C;
            }
        }

        if(LOGCOMBAT){
            logCombatSamples(startGame, scorePlayer, RCHOICES, root);
        }

        log(outputChoice(scorePlayer, root, START_TIME, bestC, sims, RCHOICES));


        return startGame.map(RCHOICES.get(bestC));
    }

    public static void logCombatSamples(MagicGame startGame, MagicPlayer scorePlayer, List<Object[]> RCHOICES, GMCTSGameTree root) {
        UUID choiceUuid = UUID.randomUUID();
        for (final GMCTSGameTree node : root) {
          Object choice[] = RCHOICES.get(node.getChoice());
          if(choice[0] instanceof MagicDeclareAttackersResult){
             if(choice.length > 1){
                throw new InvalidParameterException("Only one combat choice expected");
             }
             MagicPlayer opp = startGame.getPlayers()[(scorePlayer.getIndex()+1)%2];

             CombatScoreLog.logAttacks(
                     choiceUuid,
                     "GMCTS",
                     startGame, node.getV(),
                    node.getNumSim(),
                    node.getParent().getNumSim(),
                    scorePlayer.getLife(),
                    opp.getLife(),
                    scorePlayer.getPoison(),
                    opp.getPoison(),
                    (MagicDeclareAttackersResult) choice[0],
                    scorePlayer.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::canAttack).
                            toArray(),
                    opp.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::canBlock).
                            toArray(),
                     scorePlayer.getHandSize(),
                     opp.getHandSize(),
                     opp.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::isLand).
                            filter(MagicPermanent::isUntapped).
                            count());
          }else if(choice[0] instanceof MagicDeclareBlockersResult){
              MagicPlayer opp = startGame.getPlayers()[(scorePlayer.getIndex()+1)%2];
              if(choice.length > 1)
                  throw new RuntimeException("Multiple blocking results given");

              CombatScoreLog.logBlocks(
                      choiceUuid,
                      "GMCTS",
                      startGame,
                      node.getV(),
                      node.getNumSim(),
                      node.getParent().getNumSim(),
                      scorePlayer.getLife(),
                      opp.getLife(),
                      scorePlayer.getPoison(),
                      opp.getPoison(),
                      opp.
                              getPermanents().
                              stream().
                              filter(MagicPermanent::isAttacking).
                              toArray(),
                      (MagicDeclareBlockersResult) choice[0],
                      scorePlayer.
                              getPermanents().
                              stream().
                              filter(MagicPermanent::canBlock).
                              toArray(),

                      opp.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::isCreature).
                            toArray(),
                     scorePlayer.getHandSize(),
                     opp.getHandSize(),
                     opp.
                            getPermanents().
                            stream().
                            filter(MagicPermanent::isLand).
                            filter(MagicPermanent::isUntapped).
                            count());
          }
        }
    }

    private Runnable genSimulationTask(final MagicGame rootGame, final LinkedList<GMCTSGameTree> path, final BlockingQueue<Runnable> queue) {
        return new Runnable() {
            @Override
            public void run() {
                // propagate result of random play up the path
                final double score = randomPlay(path.getLast(), rootGame);
                queue.offer(genBackpropagationTask(score, path));
            }
        };
    }

    private Runnable genBackpropagationTask(final double score, final LinkedList<GMCTSGameTree> path) {
        return new Runnable() {
            @Override
            public void run() {
                final Iterator<GMCTSGameTree> iter = path.descendingIterator();
                GMCTSGameTree child = null;
                GMCTSGameTree parent = null;
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
        final GMCTSGameTree root,
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
        final LinkedList<GMCTSGameTree> path = growTree(root, rootGame, RCHOICES);

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
        final Iterator<GMCTSGameTree> iter = path.descendingIterator();
        GMCTSGameTree child = null;
        GMCTSGameTree parent = null;
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
        final GMCTSGameTree root,
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

        for (final GMCTSGameTree node : root) {
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

    private LinkedList<GMCTSGameTree> growTree(final GMCTSGameTree root, final MagicGame game, final List<Object[]> RCHOICES) {
        final LinkedList<GMCTSGameTree> path = new LinkedList<GMCTSGameTree>();
        boolean found = false;
        GMCTSGameTree curr = root;
        path.add(curr);

        for (List<Object[]> choices = getNextChoices(game, RCHOICES);
             !choices.isEmpty() && !Thread.currentThread().isInterrupted();
             choices = getNextChoices(game, RCHOICES)) {

            assert choices.size() > 0 : "ERROR! No choice at start of genNewTreeNode";

            final MagicEvent event = game.getNextEvent();

            //first time considering the choices available at this node,
            //fill in additional details for curr
            if (!curr.hasDetails()) {
                curr.setIsAI(game.getScorePlayer() == event.getPlayer());
                curr.setMaxChildren(choices.size());
//                System.out.println("Setting "+ choices.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
            }

            //look for first non root AI node along this path and add it to cache
            if (!found && curr != root && curr.isAI()) {
                found = true;
//                assert curr.isCached() || printPath(path);
                if(!curr.isCached())
                  GMCTSGameTree.addNode(CACHE, game, curr);

            }

            //there are unexplored children of node
            //assume we explore children of a node in increasing order of the choices
            if (curr.size() < choices.size()) {
                if(choices.size() > 2 && isCombatChoice(choices)) {
//                    System.out.println("adding preweighted choices");
                    for (final Map.Entry<Object[], Float> scoredChoice : scoredChoices(choices, game)) {
                        final GMCTSGameTree child = new GMCTSGameTree(curr, choices.indexOf(scoredChoice.getKey()), scoredChoice.getValue());
//                        System.out.println("New UCT: "+child.getModifiedUCT());
                        curr.addChild(child);
                    }
                    path.add(curr.first());

                }else {
                    final int idx = curr.size();
                    final Object[] choice = choices.get(idx);
                    game.executeNextEvent(choice);
                    final GMCTSGameTree child = new GMCTSGameTree(curr, idx, game.getScore());
                    curr.addChild(child);
                    path.add(child);
                }
                return path;

            //all the children are in the tree, find the "best" child to explore
            } else {

                GMCTSGameTree next = null;
                double bestS = Double.NEGATIVE_INFINITY ;
                for (final GMCTSGameTree child : curr) {
                    final double S = child.getModifiedUCT();
                    if (S > bestS || next == null) {
                        bestS = S;
                        next = child;
                    }
                }

                //move down the tree
                curr = next;

                //update the game state and path
                try {

                    if(choices.size() <= curr.getChoice()) {
                        System.out.println("Choices " + choices.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
                        System.out.println("Curr: " + curr.getChoice());
                    }
                    game.executeNextEvent(choices.get(curr.getChoice()));
                } catch (final IndexOutOfBoundsException ex) {
                    throw new GameException(ex, game);
                }
                path.add(curr);
            }
        }

        return path;
    }

//    private List<Object[]> nextOrderedChoices(MagicGame game, List<Object[]> RCHOICES) {
//        List<Object[]> ordered =  orderedChoices(getNextChoices(game, RCHOICES), game);
////        System.out.println("Ordered "+ ordered.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
//        return ordered;
//    }

    //returns a reward in the range [0, 1]
    private double randomPlay(final GMCTSGameTree node, final MagicGame game) {
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
          if(choice.length > 0 && (choice[0] instanceof magic.model.choice.MagicDeclareAttackersResult || choice[0] instanceof magic.model.choice.MagicDeclareBlockersResult)){
              assert choice.length == 1 : "should only have one combat choice";
              isCombat=true;
              return true;
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
//            final List<Object[]> artificialChoiceResults = ALTCHOICES ? event.getAlternativeArtificialChoiceResults(game) : event.getArtificialChoiceResults(game);
            final Object[] choice = event.getSimulationChoiceResult(game);
//            final Object[] choice = artificialChoiceResults.get(MagicRandom.nextRNGInt(artificialChoiceResults.size()));
//            Object[] bestCombatChoice= findBestCombatChoice(game, artificialChoiceResults, 0.6);
//            if(bestCombatChoice == null){
//            }else{
//               choice = bestCombatChoice;
////               System.out.println("combat choice: "+ Arrays.toString(bestCombatChoice));
//            }
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

    private List<Map.Entry<Object[], Float>> scoredChoices(List<Object[]> choices, MagicGame game){
//        System.out.println("Scorig "+choices.size()+" choices");
        final List<Map.Entry<Object[], Float>> mapped = new ArrayList<>(choices.size());
        List<Float> scores;
        if(choices.get(0)[0] instanceof magic.model.choice.MagicDeclareAttackersResult){
            final List<CombatPredictionClient.AttackRep> combatReps = new ArrayList<>(choices.size());
            final MagicPlayer scorePlayer = game.getScorePlayer();
            final MagicPlayer opp = game.getPlayers()[(scorePlayer.getIndex() + 1) % 2];
            final List<Float> availableAttackersIds = combatPredictionClient.extractPT(scorePlayer.
                    getPermanents().
                    stream().
                    filter(MagicPermanent::canAttack).
                    toArray());
            final List<Float> blockersIds = combatPredictionClient.extractPT(opp.
                    getPermanents().
                    stream().
                    filter(MagicPermanent::canBlock).
                    toArray());
            for (Object[] combatChoice : choices) {
    //            System.out.println(Arrays.toString(combatChoice));
                combatReps.add(combatPredictionClient.new AttackRep(
                        scorePlayer.getLife(),
                        opp.getLife(),
                        (MagicDeclareAttackersResult) combatChoice[0],
                        availableAttackersIds,
                        blockersIds
                ));
            }
            scores = combatPredictionClient.predictAttackWin(combatReps);
        }else if(choices.get(0)[0] instanceof magic.model.choice.MagicDeclareBlockersResult){
//            System.err.println(ExceptionReport.getGameDetails(game));
            final List<CombatPredictionClient.BlockRep> combatReps = new ArrayList<>(choices.size());
            final MagicPlayer opp = game.getTurnPlayer();
            final MagicPlayer scorePlayer = game.getPlayers()[(opp.getIndex() + 1) % 2];
            Object[] attackers = opp.
                    getPermanents().
                    stream().
                    filter(MagicPermanent::isAttacking).
                    toArray();
            Object[] availableBlockers = scorePlayer.
                    getPermanents().
                    stream().
                    filter(MagicPermanent::canBlock).
                    toArray();
//            System.err.println(Arrays.toString(availableBlockers));
//            System.err.println(Arrays.toString(opp.
//                    getPermanents().
//                    stream().
//                    filter(MagicPermanent::canBlock).
//                    toArray()));
            final List<Float> attackerIds = combatPredictionClient.extractPT(attackers);
            final List<Float> availableBlockerIds = combatPredictionClient.extractPT(availableBlockers);
            final List<Float> oppCreatureIds = combatPredictionClient.extractPT(opp.
                    getPermanents().
                    toArray());
            for (Object[] combatChoice : choices) {
    //            System.out.println(Arrays.toString(combatChoice));
                combatReps.add(combatPredictionClient.new BlockRep(
                        scorePlayer.getLife(),
                        opp.getLife(),
                        attackerIds,
                        availableBlockerIds,
                        combatPredictionClient.extractBlock(
                                (MagicDeclareBlockersResult) combatChoice[0],
                                attackers,
                                availableBlockers
                        ),
                        oppCreatureIds
                ));
            }
            scores = combatPredictionClient.predictBlockWin(combatReps);
        }else {
            System.out.println("Unknown combat choice "+choices.get(0)[0].toString());
            return null;
        }
        int ix = 0;
        for (Float score : scores) {
            Object[] choice = choices.get(ix++);
//            System.err.println(Arrays.toString(choice) + " => "+score);
            mapped.add(Maps.immutableEntry(choice, score));
        }
        return mapped;
    }

//    private PriorityQueue<Map.Entry<Object[], Float>> prioritizedChoices(List<Object[]> choices, MagicGame game){
//        final PriorityQueue<Map.Entry<Object[], Float>> choiceQueue =
//                new PriorityQueue<>(Comparator.comparing(e->-e.getValue()));
//        if(isCombatChoice(choices)) {
//
//            for(Map.Entry<Object[], Float> choice : scoredChoices(choices, game)){
//                // add small offset based on number of creatures attacking to ensure consistent ordering and favor more aggressive play
//                choiceQueue.offer(Maps.immutableEntry(choice.getKey(), choice.getValue()+ 0.0000001f * ((MagicDeclareAttackersResult) choice.getKey()[0]).size()));
//            }
//        }else{
//            for(Object[] choice : choices) {
//                final MagicGame tmpGame = new MagicGame(game, game.getScorePlayer());
//                tmpGame.executeNextEvent(choice);
//                choiceQueue.add(Maps.immutableEntry(choice, sigmoid(1.0f*tmpGame.getScore())));
//            }
//        }
//        return choiceQueue;
//    }
//    public static float sigmoid(float x) {
//      return (float) (1/( 1 + Math.pow(Math.E,(-1*x))));
//    }
//    private List<Object[]> orderedChoices(List<Object[]> choices, MagicGame game) {
//        if(choices.size() < 7 || !isCombatChoice(choices))
//            return choices;
////        PriorityQueue<Map.Entry<Object[], Float>> q =  prioritizedChoices(choices, game);
////            System.out.println("Ordered scores "+ q.stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(", ")));
//        return prioritizedChoices(choices, game).stream().map(Map.Entry::getKey).collect(Collectors.toList());
//    }

//    private Object[] findBestCombatChoice(MagicGame game, List<Object[]> choices, double scoreThreshold) {
//        if(choices.size() < 5 || !isCombatChoice(choices))
//            return null;
//        PriorityQueue<Map.Entry<Object[], Float>> queued = prioritizedChoices(choices, game);
////            System.out.println(queued.stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(", ")));
//        Map.Entry<Object[], Float> choiceMap = queued.peek();
//        if((choiceMap.getValue()) > scoreThreshold) {
//            return choiceMap.getKey();
//        }
//        return null;
//    }

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
                choices = ALTCHOICES ? event.getAlternativeArtificialChoiceResults(game) :  event.getArtificialChoiceResults(game);
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
}


//each tree node stores the choice from the parent that leads to this node
class GMCTSGameTree implements Iterable<GMCTSGameTree> {

    private final GMCTSGameTree parent;
    private final LinkedList<GMCTSGameTree> children = new LinkedList<GMCTSGameTree>();
    private final int choice;
    private boolean isAI;
    private boolean isCached;
    private int maxChildren = -1;
    private int numLose;
    private int numSim;
    private int evalScore;
    private int steps;
    private double sum;
//    private double S;

    //min sim for using robust max
    private int maxChildSim = MCTSAI.MIN_SIM;

    GMCTSGameTree(final GMCTSGameTree parent, final int choice, final int evalScore) {
        this.evalScore = evalScore;
        this.choice = choice;
        this.parent = parent;
    }

    GMCTSGameTree(final GMCTSGameTree parent, final int choice, final double initScore) {
        this.evalScore = -1;
        // the weight in number of games
        int initScoreWeight = 5;
        this.sum = initScore* initScoreWeight;
        this.numSim = initScoreWeight;
        this.choice = choice;
        this.parent = parent;
    }

    private static boolean log(final String message) {
        MagicGameLog.log(message);
        return true;
    }

    static void addNode(final LRUCache<Long, GMCTSGameTree> cache, final MagicGame game, final GMCTSGameTree node) {
        final long gid = game.getStateId();
        cache.put(gid, node);
        node.setCached();
        assert log("ADDED: " + game.getIdString());
    }

    static GMCTSGameTree getNode(final LRUCache<Long, GMCTSGameTree> cache, final MagicGame game, final List<Object[]> choices) {
        final long gid = game.getStateId();
        final GMCTSGameTree candidate = cache.get(gid);

        if (candidate != null) {
            assert log("CACHE HIT");
            assert log("HIT  : " + game.getIdString());
            //assert printNode(candidate, choices);
            return candidate;
        } else {
            assert log("CACHE MISS");
            assert log("MISS : " + game.getIdString());
            final GMCTSGameTree root = new GMCTSGameTree(null, -1, -1);
            return root;
        }
    }

    boolean isCached() {
        return isCached;
    }

    private void setCached() {
        isCached = true;
    }

    boolean hasDetails() {
        return maxChildren != -1;
    }

    void setMaxChildren(final int mc) {
        maxChildren = mc;
    }

    boolean isAI() {
        return isAI;
    }

    boolean isOpp() {
        return !isAI;
    }

    void setIsAI(final boolean ai) {
        this.isAI = ai;
    }

    boolean isSolved() {
        return evalScore == Integer.MAX_VALUE || evalScore == Integer.MIN_VALUE;
    }

    void recordVirtualLoss() {
        numSim++;
    }

    void removeVirtualLoss() {
        numSim--;
    }

    void updateScore(final GMCTSGameTree child, final double delta) {
//        final double oldMean = (numSim > 0) ? sum/numSim : 0;
        assert !(delta < 0 || delta > 1) : "invalid delta "+delta;
        sum += delta;
        numSim += 1;
//        final double newMean = sum/numSim;
//        S += (delta - oldMean) * (delta - newMean);

        //if child has sufficient simulations, backup using robust max instead of average
        if (child != null && child.getNumSim() > maxChildSim) {
            maxChildSim = child.getNumSim();
            sum = child.sum;
            numSim = child.numSim;
        }
    }

    double getModifiedUCT() {
//        final double v = getV();
//        final double sc = v*v + MCTSAI.UCB1_C * Math.log(parent.getNumSim()) / getNumSim();
        final double sc = getV() + MCTSAI.UCB1_C * Math.sqrt(Math.log(parent.getNumSim()) / getNumSim());
        if ((!parent.isAI() && isAIWin()) || (parent.isAI() && isAILose())) {
            return sc - 2.0;
        } else if ((parent.isAI() && isAIWin()) || (!parent.isAI() && isAILose())) {
            return sc + 2.0;
        } else {
            return sc;
        }
    }

    boolean isAIWin() {
        return evalScore == Integer.MAX_VALUE;
    }

    boolean isAILose() {
        return evalScore == Integer.MIN_VALUE;
    }

    void incLose(final int lsteps) {
        numLose++;
        steps = Math.max(steps, lsteps);
        if (numLose == maxChildren) {
            if (isAI) {
                setAILose(steps);
            } else {
                setAIWin(steps);
            }
        }
    }

    int getChoice() {
        return choice;
    }

    int getSteps() {
        return steps;
    }

    void setAIWin(final int aSteps) {
        evalScore = Integer.MAX_VALUE;
        steps = aSteps;
    }

    void setAILose(final int aSteps) {
        evalScore = Integer.MIN_VALUE;
        steps = aSteps;
    }

    double getDecision() {
        //boost decision score of win nodes by BOOST
        final int BOOST = 1000000;
        if (isAIWin()) {
            return BOOST + getNumSim();
        } else if (isAILose()) {
            return getNumSim();
        } else {
            return getNumSim();
        }
    }

    int getNumSim() {
        return numSim;
    }

    private double getSum() {
        // AI is max player, other is min player
        return parent.isAI() ? sum : -sum;
    }

    double getV() {
        return getSum() / numSim;
    }
    public GMCTSGameTree getParent() { return this.parent; }

    void addChild(final GMCTSGameTree child) {
        assert children.size() < maxChildren : "ERROR! Number of children nodes exceed maxChildren";
        children.add(child);
    }

    GMCTSGameTree first() {
        return children.peek();
    }

    @Override
    public Iterator<GMCTSGameTree> iterator() {
        return children.iterator();
    }

    int size() {
        return children.size();
    }
}