def EFFECT1 = MagicRuleEventAction.create("SN deals 2 damage to target creature or player.");

def EFFECT2 = MagicRuleEventAction.create("SN deals 4 damage to target blue creature.");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    MagicTargetChoice.NEG_TARGET_CREATURE_OR_PLAYER,
                    MagicTargetChoice.Negative("Target blue creature")
                ),
                this,
                "Choose one\$ - SN deals 2 damage to target creature or player; " +
                "or SN deals 4 damage to target blue creature."
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
