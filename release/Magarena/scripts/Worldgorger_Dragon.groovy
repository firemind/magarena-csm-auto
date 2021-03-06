[
    new MagicWhenComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicPayedCost payedCost) {
            return new MagicEvent(
                permanent,
                this,
                "Exile all other permanents you control."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicTargetFilter<MagicPermanent> permanents =
                new MagicOtherPermanentTargetFilter(
                MagicTargetFilterFactory.PERMANENT_YOU_CONTROL,event.getPermanent());
            final Collection<MagicPermanent> targets=
                game.filterPermanents(permanents);
            for (final MagicPermanent target : targets) {
                game.doAction(new MagicExileLinkAction(
                    event.getPermanent(),
                    target
                ));
        }
    }
},
    new MagicWhenLeavesPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent, final MagicRemoveFromPlayAction act) {
            return new MagicEvent(
                permanent,
                this,
                "Return the exiled cards to the battlefield under their owners' control."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new MagicReturnLinkedExileAction(
                event.getPermanent(),
                MagicLocationType.Play,
                event.getPlayer()
            ));
        }
    }
]
