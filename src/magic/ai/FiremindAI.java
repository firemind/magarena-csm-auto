package magic.ai;

import java.util.*;

import magic.model.MagicGame;
import magic.model.MagicGameLog;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.event.MagicEvent;
import magic.firemind.CombatPredictionClient;

public class FiremindAI extends MagicAI {

    private final boolean CHEAT;
    private CombatPredictionClient combatPredictionClient;
    public FiremindAI(final boolean aCheat) {
        CHEAT = aCheat;
        combatPredictionClient = new CombatPredictionClient();
    }

    @Override
    public Object[] findNextEventChoiceResults(final MagicGame sourceGame, final MagicPlayer scorePlayer) {
        final int artificialLevel = scorePlayer.getAiProfile().getAiLevel();
        final long startTime = System.currentTimeMillis();

        final MagicGame root = new MagicGame(sourceGame, scorePlayer);
        //root.setMainPhases(artificialLevel);

        if (!CHEAT) {
            root.hideHiddenCards();
        }


        final MagicEvent event = root.getNextEvent();
        final List<Object[]> choices = event.getArtificialChoiceResults(root);
        if (choices.size() == 1) {
            return sourceGame.map(choices.get(0));
        }
        return sourceGame.map(choices.get(new Random().nextInt(choices.size())));

    }

    private void log(final String message) {
        MagicGameLog.log(message);
    }
}
