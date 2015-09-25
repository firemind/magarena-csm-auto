[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                NEG_TARGET_ARTIFACT,
                MagicDestroyTargetPicker.DestroyNoRegen,
                this,
                "Destroy target artifact\$. It can't be regenerated. " +
                "Put X 1/1 green Saproling creature tokens onto the battlefield, where X is that artifact's converted mana cost."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(ChangeStateAction.Set(
                    it,
                    MagicPermanentState.CannotBeRegenerated
                ));
                game.doAction(new DestroyAction(it));
                game.doAction(new PlayTokensAction(
                    event.getPlayer(),
                    CardDefinitions.getToken("1/1 green Saproling creature token"),
                    it.getConvertedCost()
                ));
            });
        }
    }
]
