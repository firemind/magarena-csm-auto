def EFFECT1 = MagicRuleEventAction.create("All creatures get -1/-1 until end of turn.");

def EFFECT2 = MagicRuleEventAction.create("Destroy target enchantment.");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    MagicTargetChoice.NONE,
                    MagicTargetChoice.NEG_TARGET_ENCHANTMENT,
                    MagicTargetChoice.NONE
                ),
                this,
                "Choose one\$ - all creatures get -1/-1 until end of turn; " +
                "or destroy target enchantment; " +
                "or regenerate each creature you control.\$" 
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                game.addEvent(EFFECT1.getEvent(event.getSource()));
            } else if (event.isMode(2)) {
                game.addEvent(EFFECT2.getEvent(event.getSource()));
            } else if (event.isMode(3)) {
            final Collection<MagicPermanent> targets=
            game.filterPermanents(event.getPlayer(),MagicTargetFilterFactory.CREATURE_YOU_CONTROL);
            for (final MagicPermanent permanent : targets) {
                game.doAction(new MagicRegenerateAction(permanent));
                }
            }
        }
    }
]
