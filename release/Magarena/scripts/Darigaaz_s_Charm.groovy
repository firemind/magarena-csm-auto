def EFFECT1 = MagicRuleEventAction.create("Return target creature card from your graveyard to your hand.");

def EFFECT2 = MagicRuleEventAction.create("SN deals 3 damage to target creature or player.");

def EFFECT3 = MagicRuleEventAction.create("Target creature gets +3/+3 until end of turn.");


[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    MagicTargetChoice.Positive("a creature card from your graveyard"),
                    MagicTargetChoice.NEG_TARGET_CREATURE_OR_PLAYER,
                    MagicTargetChoice.POS_TARGET_CREATURE
                ),
                this,
                "Choose one\$ - return target creature card from your graveyard to your hand; " +
                "or SN deals 3 damage to target creature or player; " +
                "or target creature gets +3/+3 until end of turn." 
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                game.addEvent(EFFECT1.getEvent(event.getSource()));
            } else if (event.isMode(2)) {
                game.addEvent(EFFECT2.getEvent(event.getSource()));
            } else if (event.isMode(3)) {
                game.addEvent(EFFECT3.getEvent(event.getSource()));
            }
        }
    }
]
