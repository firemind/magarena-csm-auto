package magic.model.choice;

import magic.model.MagicAbility;
import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;

import java.util.*;

public class MagicAlternativeDeclareAttackersResultBuilder {

    private static final Collection<Object> EMPTY_RESULT=Collections.<Object>singletonList(new MagicDeclareAttackersResult());

    static Collection<Object> buildResults(final MagicGame game, final MagicPlayer attackingPlayer) {
        final MagicPlayer defendingPlayer = attackingPlayer.getOpponent();
        final MagicCombatCreatureBuilder creatureBuilder=new MagicCombatCreatureBuilder(game,attackingPlayer,defendingPlayer);
        creatureBuilder.buildBlockers();

        // Check if none of the attacking player's creatures can attack.
        if (!creatureBuilder.buildAttackers()) {
            return EMPTY_RESULT;
        }

        // Remove creatures that have zero power.
        // Remove creatures that must attack if able and add them to result.
        final SortedSet<MagicCombatCreature> attackersSet=creatureBuilder.getAttackers();
        final MagicPermanent[] current=new MagicPermanent[attackersSet.size()];
        int count=0;
        for (final Iterator<MagicCombatCreature> iterator=attackersSet.iterator();iterator.hasNext();) {
            final MagicCombatCreature attacker=iterator.next();
            if (attacker.hasAbility(MagicAbility.AttacksEachTurnIfAble)) {
                current[count++]=attacker.permanent;
                iterator.remove();
            } else if (attacker.power<=0) {
                iterator.remove();
            }
        }


        // Build results.
        final Collection<Object> results= new ArrayList<>();
        results.add(new MagicDeclareAttackersResult(current,count));

        for(MagicCombatCreature attacker : attackersSet){
            final Collection<Object> subSet= new ArrayList<>();
            for(Object res : results){
                MagicDeclareAttackersResult attackerRes = new MagicDeclareAttackersResult();
                attackerRes.addAll((Collection<MagicPermanent>) res);
                attackerRes.add(attacker.permanent);
                subSet.add(attackerRes);
            }
            results.addAll(subSet);
        }

        return results;
    }
}
