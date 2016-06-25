def action = {
    final MagicGame game, final MagicEvent event ->
    if (event.isYes()) {
        game.doAction(new MagicDealDamageAction(event.getSource(),event.getPlayer(),5));
    } else {
        game.doAction(new MagicDrawAction(event.getRefPlayer(),3));
    }
}

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                MagicTargetChoice.POS_TARGET_PLAYER,
                this,
                "Your opponent may have SN deal 5 damage to him or her. If he or she doesn't, target player\$ draws three cards."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPlayer(game, {
                game.addEvent(new MagicEvent(
                    event.getSource(),
                    event.getPlayer().getOpponent(),
                    new MagicMayChoice("have SN deal 5 damage to you?"),
                    it,
                    action,
                    "PN may\$ have SN deal 5 damage to you. If you don't, your opponent draws 3 cards."
                ));
            });
        }
    }
]
