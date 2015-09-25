[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                TARGET_CREATURE,
                MagicDestroyTargetPicker.DestroyNoRegen,
                this,
                "Destroy target creature\$. It can't be regenerated. " +
                "Its controller puts a 1/1 white Spirit creature " +
                "token with flying onto the battlefield."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                final MagicPlayer controller=it.getController();
                game.doAction(ChangeStateAction.Set(
                    it,
                    MagicPermanentState.CannotBeRegenerated
                ));
                game.doAction(new DestroyAction(it));
                game.doAction(new PlayTokenAction(
                    controller,
                    CardDefinitions.getToken("1/1 white Spirit creature token with flying")
                ));
            });
        }
    }
]
