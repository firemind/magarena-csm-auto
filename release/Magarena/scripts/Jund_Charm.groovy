def EFFECT2 = MagicRuleEventAction.create("SN deals 2 damage to each creature.");

def EFFECT3 = MagicRuleEventAction.create("Put two +1/+1 counters on target creature.");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    MagicTargetChoice.NEG_TARGET_PLAYER,
                    MagicTargetChoice.NONE,
                    MagicTargetChoice.POS_TARGET_PLAYER
                ),
                this,
                "Choose one\$ - exile all cards from target player's graveyard; " +
                "or SN deals 2 damage to each creature; " +
                "or put two +1/+1 counters on target creature.\$" 
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                event.processTargetPlayer(game, {
                final MagicCardList graveyard = new MagicCardList(it.getGraveyard());
                for (final MagicCard cardGraveyard : graveyard) {
                    game.doAction(new MagicRemoveCardAction(cardGraveyard,MagicLocationType.Graveyard));
                    game.doAction(new MagicMoveCardAction(cardGraveyard,MagicLocationType.Graveyard,MagicLocationType.Exile));
                }
            });
            } else if (event.isMode(2)) {
                game.addEvent(EFFECT2.getEvent(event.getSource()));
            } else if (event.isMode(3)) {
                game.addEvent(EFFECT3.getEvent(event.getSource()));
            }
        }
    }
]
