[
    new OtherDiesTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPermanent otherPermanent) {
            return (otherPermanent.hasSubType(MagicSubType.Bird) && 
                    otherPermanent.getCard().isFriend(permanent)) ?
                new MagicEvent(
                    permanent,
                    this,
                    "Put a feather counter on SN."
                ) :
                MagicEvent.NONE;
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new ChangeCountersAction(
                event.getPermanent(),
                MagicCounterType.Feather,
                1
            ));
        }
    }
]
