[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                payedCost.isKicked() ? 
                    MagicTargetChoice.NEG_TARGET_PLAYER :
                    new MagicOrChoice(
                        MagicTargetChoice.NEG_TARGET_PLAYER,
                        MagicChoice.NONE
                    ),
                this,
                payedCost.isKicked() ?
                    "Creatures target player\$ controls attack this turn if able. "+ 
                    "Creatures you control gain first strike until end of turn." :
                    "Choose one\$ — • Creatures target player\$ controls attack this turn if able. "+
                    "• Creatures you control gain first strike until end of turn."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isKicked() || event.isMode(1)) {
                event.processTargetPlayer(game, {
                    final Collection<MagicPermanent> targets = game.filterPermanents(
                        it,
                        MagicTargetFilterFactory.CREATURE_YOU_CONTROL
                    );
                    for (final MagicPermanent creature : targets) {
                        game.doAction(new MagicGainAbilityAction(creature,MagicAbility.AttacksEachTurnIfAble));
                    }
                });
            }
            if (event.isKicked() || event.isMode(2)) {
                final Collection<MagicPermanent> targets = game.filterPermanents(
                    event.getPlayer(),
                    MagicTargetFilterFactory.CREATURE_YOU_CONTROL
                );
                for (final MagicPermanent creature : targets) {
                    game.doAction(new MagicGainAbilityAction(creature,MagicAbility.FirstStrike));
                }
            }
        }
    }
]
