[
    new MagicWhenComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicPayedCost payedCost) {
            return new MagicEvent(
                permanent,
                MagicTargetChoice.NEG_TARGET_CREATURE,
                MagicBounceTargetPicker.create(),
                this,
                "Put target creature\$ on top of its owner's library."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                final MagicPermanent creature ->
                game.doAction(new MagicRemoveFromPlayAction(creature,MagicLocationType.TopOfOwnersLibrary));
            });
        }
    }
]
