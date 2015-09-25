[
    new MagicWhenAttacksTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPermanent creature) {
            return (creature == permanent && game.getAttackingPlayer().getNrOfAttackers()==1) ?
                new MagicEvent(
                    permanent,
                    this,
                    "SN gets +3/+0 until end of turn."
                ):
                MagicEvent.NONE;
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new ChangeTurnPTAction(event.getPermanent(),3,0));
        }
    }
]
