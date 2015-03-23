[
    new MagicWhenSelfLeavesPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicRemoveFromPlayAction act) {
            return new MagicEvent(
                permanent,
                MagicTargetChoice.TARGET_PLAYER,
                this,
                "PN puts a +1/+1 counter on each creature target player\$ controls."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPlayer(game, {
                final Collection<MagicPermanent> targets = it.filterPermanents(MagicTargetFilterFactory.CREATURE_YOU_CONTROL);
                for (final MagicPermanent target : targets) {
                    game.doAction(new MagicChangeCountersAction(target,MagicCounterType.PlusOne,1));
                }
            });
        }
    }
]
