[
    new MagicStatic(MagicLayer.ModPT, CREATURE) {
        @Override
        public void modPowerToughness(final MagicPermanent source,final MagicPermanent permanent,final MagicPowerToughness pt) {
            final int amount = CREATURE_CARD_FROM_GRAVEYARD.filter(source).size();
            pt.add(amount,amount);
        }
        @Override
        public boolean condition(final MagicGame game,final MagicPermanent source,final MagicPermanent target) {
            return target == source.getEnchantedPermanent();
        }
    }
]
