package magic.model.event;

import java.util.Collection;

import magic.model.MagicGame;
import magic.model.MagicLocationType;
import magic.model.MagicMessage;
import magic.model.MagicPermanent;
import magic.model.MagicSource;
import magic.model.action.RemoveFromPlayAction;
import magic.model.choice.MagicTargetChoice;
import magic.model.target.MagicTargetFilter;
import magic.model.target.MagicTargetHint;

public class MagicUniquenessEvent extends MagicEvent {

    public MagicUniquenessEvent(final MagicSource source, final MagicTargetFilter<MagicPermanent> filter) {
        super(
            source,
            new MagicTargetChoice(
                filter,
                MagicTargetHint.None,
                "one. Put the rest into their owner's graveyard"
            ),
            EVENT_ACTION,
            "Choose one$. Put the rest into their owner's graveyard."
        );
    }

    private static final MagicEventAction EVENT_ACTION = (final MagicGame game, final MagicEvent event) ->
        event.processTargetPermanent(game, (final MagicPermanent permanent) -> {
            final Collection<MagicPermanent> targets = event.getTargetChoice().getPermanentFilter().filter(event);
            for (final MagicPermanent target : targets) {
                if (target != permanent) {
                    game.logAppendMessage(
                        event.getPlayer(),
                        MagicMessage.format("Put %s into its owner's graveyard (Uniqueness rule).", target)
                    );
                    game.doAction(new RemoveFromPlayAction(target,MagicLocationType.Graveyard));
                }
            }
        });
}
