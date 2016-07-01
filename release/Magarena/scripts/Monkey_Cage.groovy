[
    new OtherEntersBattlefieldTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPermanent otherPermanent) {
            return otherPermanent.isCreature() ?
                new MagicEvent(
                    permanent,
                    otherPermanent,
                    this,
                    "PN puts X 2/2 green Ape creature tokens onto the battlefield, where X is RN's converted mana cost."
                ):
                MagicEvent.NONE;
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicPlayer player = event.getPlayer();
            final int amount = event.getRefPermanent().getConvertedCost();
            game.logAppendValue(player, amount);
            game.doAction(new SacrificeAction(event.getPermanent()));
            game.doAction(new PlayTokensAction(
                player,
                CardDefinitions.getToken("2/2 green Ape creature token"),
                amount
            ));
        }
    }
]
