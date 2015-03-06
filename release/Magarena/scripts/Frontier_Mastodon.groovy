[
    new MagicWhenComesIntoPlayTrigger(MagicTrigger.REPLACEMENT) {
        @Override
        public MagicEvent executeTrigger(final MagicGame game, final MagicPermanent permanent, final MagicPayedCost payedCost) {
            if (permanent.getController().controlsPermanent(MagicTargetFilterFactory.CREATURE_POWER_4_OR_MORE)) {
                game.doAction(MagicChangeCountersAction.Enters(permanent,MagicCounterType.PlusOne,1));
            }
            return MagicEvent.NONE;
        }
    }
]