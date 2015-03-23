[
    new MagicWhenSelfLeavesPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicRemoveFromPlayAction act) {
            return new MagicEvent(
                permanent,
                permanent.getCounters(MagicCounterType.Age),
                this,
                "PN puts RN 1/1 white Bird creature tokens with flying onto the battlefield."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new MagicPlayTokensAction(
                event.getPlayer(),
                TokenCardDefinitions.get("1/1 white Bird creature token with flying"),
                event.getRefInt()
            ));
        }
    }
]
