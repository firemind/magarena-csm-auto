def MERFOLK_YOU_CONTROL = new MagicPermanentFilterImpl() {
    public boolean accept(final MagicGame game,final MagicPlayer player,final MagicPermanent target) {
        return target.hasSubType(MagicSubType.Merfolk) && target.isController(player);
    } 
};

def TARGET_MERFOLK_YOU_CONTROL = new MagicTargetChoice(
    MERFOLK_YOU_CONTROL,
    "target Merfolk you control"
);


def action = {
    final MagicGame game, final MagicEvent event ->
    game.doAction(new MagicGainAbilityAction(
        event.getRefPermanent(),
        event.getChosenColor().getProtectionAbility()
    ));
}

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Pump),
        "Protection"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [new MagicPayManaCostEvent(source,"{1}{W}")];
        }
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                TARGET_MERFOLK_YOU_CONTROL,
                this,
                "Target Merfolk\$ you control gains protection from the color of your choice until end of turn."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.addEvent(new MagicEvent(
                    event.getSource(),
                    event.getPlayer(),
                    MagicColorChoice.ALL_INSTANCE,
                    it,
                    action,
                    "Chosen color\$."
                ));
            });
        }
    }
]
