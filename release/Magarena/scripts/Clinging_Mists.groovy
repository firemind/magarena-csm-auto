[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                this,
                "Prevent all combat damage that would be dealt this turn. " +
                "If you have 5 or less life, tap all attacking creatures. Those creatures don't untap during their controller's next untap step."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new AddTurnTriggerAction(
                MagicPreventDamageTrigger.PreventCombatDamage
            ));
            if (MagicCondition.FATEFUL_HOUR.accept(event.getSource())) {
                ATTACKING_CREATURE.filter(event) each {
                    game.doAction(new TapAction(it));
                    game.doAction(ChangeStateAction.Set(
                        it,
                        MagicPermanentState.DoesNotUntapDuringNext
                    ));
                }
            }
        }
    }
]
