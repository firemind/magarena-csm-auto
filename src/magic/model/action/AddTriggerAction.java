package magic.model.action;

import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.trigger.MagicTrigger;

public class AddTriggerAction extends MagicAction {

    private final MagicPermanent permanent;
    private final MagicTrigger<?> trigger;
    private final boolean force;

    private AddTriggerAction(final MagicPermanent aPermanent,final MagicTrigger<?> aTrigger,final boolean aForce) {
        permanent = aPermanent;
        trigger = aTrigger;
        force = aForce;
    }

    public AddTriggerAction(final MagicPermanent aPermanent,final MagicTrigger<?> aTrigger) {
        this(aPermanent, aTrigger, false);
    }

    public AddTriggerAction(final MagicTrigger<?> trigger) {
        this(MagicPermanent.NONE, trigger);
    }

    public static AddTriggerAction Force(final MagicPermanent aPermanent,final MagicTrigger<?> aTrigger) {
        return new AddTriggerAction(aPermanent, aTrigger, true);
    }

    @Override
    public boolean isLegal(final MagicGame game) {
        return permanent == MagicPermanent.NONE || permanent.isValid() || force;
    }

    @Override
    public void doAction(final MagicGame game) {
        game.addTrigger(permanent, trigger);
    }

    @Override
    public void undoAction(final MagicGame game) {
        game.removeTrigger(permanent, trigger);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" ("+permanent+','+trigger+')';
    }
}
