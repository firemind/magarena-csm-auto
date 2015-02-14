def TAP_EFFECT = MagicRuleEventAction.create("Tap target permanent an opponent controls.");

def TapTrigger = new MagicWhenComesIntoPlayTrigger() {
    @Override
    public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicPayedCost payedCost) {
        return TAP_EFFECT.getEvent(permanent);
    }
}

def UNTAP_EFFECT = MagicRuleEventAction.create("Untap target permanent you control.");

def UntapTrigger = new MagicWhenComesIntoPlayTrigger() {
    @Override
    public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicPayedCost payedCost) {
        return UNTAP_EFFECT.getEvent(permanent);
    }
}

[
    new MagicWhenComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicPayedCost payedCost) {
            return new MagicEvent(
                permanent,
                new MagicOrChoice(
                    MagicTargetChoice.TARGET_PERMANENT_YOU_CONTROL,
                    MagicTargetChoice.TARGET_PERMANENT_AN_OPPONENT_CONTROLS
                ),
                payedCost,
                this,
                "Choose one\$ - Untap target permanent you control; or tap target permanent an opponent controls."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                game.executeTrigger(UntapTrigger, event.getPermanent(), event.getSource(), event.getRefPayedCost());
            } else if (event.isMode(2)) {
                game.executeTrigger(TapTrigger, event.getPermanent(), event.getSource(), event.getRefPayedCost());
            }
        }
        @Override
        public boolean usesStack() {
            return false;
        }
    }
]

