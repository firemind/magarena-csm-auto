[
    new MagicWhenOtherComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPermanent otherPermanent) {
            return (otherPermanent.getController() == permanent.getController() &&
                    otherPermanent.hasSubType(MagicSubType.Ally)) ?
                new MagicEvent(
                    permanent,
                    new MagicSimpleMayChoice(),
                    this,
                    "PN may\$ put a 2/2 green Wolf creature " +
                    "token onto the battlefield. If you do, put a " +
                    "+1/+1 counter on SN."
                ) :
                MagicEvent.NONE;
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isYes()) {
                game.doAction(new PlayTokenAction(
                    event.getPlayer(),
                    CardDefinitions.getToken("2/2 green Wolf creature token")
                ));
                game.doAction(new ChangeCountersAction(event.getPermanent(),MagicCounterType.PlusOne,1));
            }
        }
    }
]
