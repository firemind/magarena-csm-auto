def EFFECT1 = MagicRuleEventAction.create("Destroy target blue permanent.");

def EFFECT2 = MagicRuleEventAction.create("Return target island to its owner's hand.");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    MagicTargetChoice.Negative("target blue permanent"),
                    MagicTargetChoice.Negative("target Island")
                ),
                this,
                "Choose one\$ - destroy target blue permanent; " +
                "or return target Island to its owner's hand."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                game.addEvent(EFFECT1.getEvent(event.getSource()));

            } else if (event.isMode(2)) {
                game.addEvent(EFFECT2.getEvent(event.getSource()));
            }
        }
    }
]
