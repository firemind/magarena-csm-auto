[
    new MagicWhenSelfLeavesPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicRemoveFromPlayAction act) {
            return new MagicEvent(
                permanent,
                this,
                "PN gains 2 life for each age counter on SN."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new MagicChangeLifeAction(
                event.getPlayer(),
                event.getPermanent().getCounters(MagicCounterType.Age) * 2
            ));
        }
    }
]
