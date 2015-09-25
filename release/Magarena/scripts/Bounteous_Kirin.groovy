[
    new MagicWhenYouCastSpiritOrArcaneTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game, final MagicPermanent permanent, final MagicCardOnStack cardOnStack) {
            return new MagicEvent(
                permanent,
                new MagicSimpleMayChoice(),
                cardOnStack.getConvertedCost(),
                this,
                "You may gain RN life."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isYes()) {
                game.doAction(new ChangeLifeAction(event.getPlayer(), event.getRefInt()));
            }
        }
    }
]
