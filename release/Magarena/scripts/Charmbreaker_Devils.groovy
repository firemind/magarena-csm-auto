[
    new MagicAtYourUpkeepTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPlayer upkeepPlayer) {
            return new MagicEvent(
                permanent,
                this,
                "Return an instant or sorcery card at random from your graveyard to your hand."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicPlayer player = event.getPlayer();
            final MagicCardList cards = new MagicCardList(INSTANT_OR_SORCERY_CARD_FROM_GRAVEYARD.filter(event));
            for (final MagicCard card : cards.getRandomCards(1)) {
                game.doAction(new ShiftCardAction(
                    card,
                    MagicLocationType.Graveyard,
                    MagicLocationType.OwnersHand
                ));
            }
        }
    }
]
