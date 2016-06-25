[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.MustAttack),
        "Attacks"
    ) {

        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                    new MagicPayManaCostEvent(source,"{2}"),                  
                    new MagicTapEvent(source)
                ];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                MagicTargetChoice.NEG_TARGET_CREATURE,
                MagicMustAttackTargetPicker.create(),
                this,
                "SN deals 1 damage to target creature\$. That creature attacks this turn if able."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new MagicDealDamageAction(event.getSource(),it,2));
                game.doAction(new MagicGainAbilityAction(it,MagicAbility.AttacksEachTurnIfAble));
            });
        }
    }
]
