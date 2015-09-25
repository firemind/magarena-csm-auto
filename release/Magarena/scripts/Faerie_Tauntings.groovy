[
    new MagicWhenOtherSpellIsCastTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicCardOnStack cardOnStack) {
            return (permanent.isFriend(cardOnStack) && permanent.isOpponent(game.getTurnPlayer())) ?
                new MagicEvent(
                    permanent,
                    new MagicMayChoice(),
                    this,
                    "You may\$ have each opponent loses 1 life."
                ): MagicEvent.NONE;
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isYes()) {
                game.doAction(new ChangeLifeAction(event.getPlayer().getOpponent(),-1));
            }
        }
    }
]
