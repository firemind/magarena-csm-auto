[
    new MagicPermanentActivation(
        [MagicCondition.YOUR_UPKEEP_CONDITION],
        new MagicActivationHints(MagicTiming.Draw),
        "Draw"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                new MagicTapEvent(source)
            ];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source, final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                MagicTargetChoice.TARGET_OPPONENT,
                this,
                "PN draws two cards. Target opponent\$ gains control of SN."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new MagicDrawAction(event.getPlayer(),2));
            event.processTargetPlayer(game, {
                game.doAction(new MagicGainControlAction(it,event.getPermanent()));
            });
        }
    }
]
