package magic.model.choice;

import java.util.*;

import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;

public class MagicAlternativeCombatCreatureBuilder {

    private static final Comparator<MagicCombatCreature> ATTACKER_COMPARATOR=new Comparator<MagicCombatCreature>() {
        @Override
        public int compare(final MagicCombatCreature attacker1,final MagicCombatCreature attacker2) {
            return Long.signum(attacker1.permanent.getStateId() - attacker2.permanent.getStateId());
//            return attacker1.permanent.compareTo(attacker2.permanent);
        }
    };

    private static final Comparator<MagicCombatCreature> BLOCKER_COMPARATOR=new Comparator<MagicCombatCreature>() {
        @Override
        public int compare(final MagicCombatCreature blocker1,final MagicCombatCreature blocker2) {
            return Long.signum(blocker1.permanent.getStateId() - blocker2.permanent.getStateId());
        }
    };

    private final MagicGame game;
    private final MagicPlayer attackingPlayer;
    private final MagicPlayer defendingPlayer;
    private TreeMap<MagicCombatCreature, List<MagicCombatCreature>> attackers;
    private TreeMap<MagicCombatCreature, List<MagicCombatCreature>> blockers;

    MagicAlternativeCombatCreatureBuilder(final MagicGame game, final MagicPlayer attackingPlayer, final MagicPlayer defendingPlayer) {
        this.game=game;
        this.attackingPlayer=attackingPlayer;
        this.defendingPlayer=defendingPlayer;
    }

    /** Must be called before building attackers. */
    boolean buildBlockers() {
        blockers=new TreeMap<MagicCombatCreature, List<MagicCombatCreature>>(BLOCKER_COMPARATOR);
        for (final MagicPermanent permanent : defendingPlayer.getPermanents()) {
            if (permanent.canBlock()) {
                MagicCombatCreature blocker = new MagicCombatCreature(permanent);
                List<MagicCombatCreature> list = blockers.getOrDefault(blocker, new ArrayList<MagicCombatCreature>());
                list.add(blocker);
                blockers.put(blocker, list);
            }
        }
        return blockers.size()>0;
    }

    private MagicCombatCreature createAttacker(final MagicPermanent permanent) {
        final MagicCombatCreature attacker=new MagicCombatCreature(permanent);
        attacker.setAttacker(game,blockers.keySet());
        return attacker;
    }

    boolean buildAttackers() {
        attackers=new TreeMap<MagicCombatCreature, List<MagicCombatCreature>>(ATTACKER_COMPARATOR);
        for (final MagicPermanent permanent : attackingPlayer.getPermanents()) {
            if (permanent.canAttack()) {
                MagicCombatCreature mcc = createAttacker(permanent);
                List<MagicCombatCreature> list = attackers.getOrDefault(mcc, new ArrayList<MagicCombatCreature>());
                list.add(mcc);
                attackers.put(mcc, list);
            }
        }
        return attackers.size()>0;
    }

    boolean buildBlockableAttackers() {
        attackers=new TreeMap<MagicCombatCreature, List<MagicCombatCreature>>(ATTACKER_COMPARATOR);
        for (final MagicPermanent permanent : attackingPlayer.getPermanents()) {
            if (permanent.isAttacking()&&permanent.canBeBlocked(defendingPlayer)) {
                final MagicCombatCreature attacker=createAttacker(permanent);
                if (attacker.candidateBlockers.length>0) {
                    List<MagicCombatCreature> list = attackers.getOrDefault(attacker, new ArrayList<MagicCombatCreature>());
                    list.add(attacker);
                    attackers.put(attacker, list);
                }
            }
        }
        return attackers.size()>0;
    }

    TreeMap<MagicCombatCreature, List<MagicCombatCreature>> getAttackers() {
        return attackers;
    }

    TreeMap<MagicCombatCreature, List<MagicCombatCreature>> getBlockers() {
        return blockers;
    }

    Set<MagicPermanent> getCandidateBlockers() {
        final Set<MagicPermanent> candidateBlockers=new HashSet<MagicPermanent>();
        for (final MagicCombatCreature attacker : attackers.keySet()) {
            for (final MagicCombatCreature blocker : attacker.candidateBlockers) {
                candidateBlockers.add(blocker.permanent);
            }
        }
        return candidateBlockers;
    }

    Set<MagicPermanent> getBlockableAttackers(final MagicPermanent blocker) {
        final Set<MagicPermanent> blockableAttackers=new HashSet<MagicPermanent>();
        for (final MagicCombatCreature attacker : attackers.keySet()) {
            for (final MagicCombatCreature candidateBlocker : attacker.candidateBlockers) {
                if (candidateBlocker.permanent==blocker) {
                    blockableAttackers.add(attacker.permanent);
                    break;
                }
            }
        }
        return blockableAttackers;
    }
}
