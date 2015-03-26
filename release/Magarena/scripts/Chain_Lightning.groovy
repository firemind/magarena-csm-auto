def action = {
    final MagicGame game, final MagicEvent event ->
    if (event.isYes()) {
        game.doAction(new MagicCopyCardOnStackAction(event.getPlayer(),event.getRefCardOnStack()));
    }
}

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                MagicTargetChoice.NEG_TARGET_CREATURE_OR_PLAYER,
                this,
                "SN deals 3 damage to target creature or player\$."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTarget(game, {
                game.addEvent(new MagicEvent(
                    event.getSource(),
                    event.getPlayer().getOpponent(),
                    new MagicMayChoice(
                        "Pay {R}{R}?",
                        new MagicPayManaCostChoice(MagicManaCost.create("{R}{R}"))
                    ),
                    event.getCardOnStack(),
                    action,
                    "PN may\$ pay {R}{R}\$. If you do, copy this spell and may choose a new target for that copy."
                ));
                game.doAction(new MagicDealDamageAction(event.getSource(),it,3));
            });
        }
    }
]
