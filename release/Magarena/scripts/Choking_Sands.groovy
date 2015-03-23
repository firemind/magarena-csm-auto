def choice = MagicTargetChoice.Negative("target non-Swamp land");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                choice,
                MagicDestroyTargetPicker.Destroy,
                this,
                "Destroy target non-Swamp land\$. If that land was nonbasic, SN deals 2 damage to that land's controller."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new MagicDestroyAction(it));
                if (it.isBasic() == false) {
                    game.doAction(new MagicDealDamageAction(
                        event.getSource(),
                        it.getController(),
                        2
                    ));
                }
            });
        }
    }
]
