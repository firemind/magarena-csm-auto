package magic.ai;

public enum MagicAIImpl {
    MMAB("minimax", new MMAB(false)),
    MMABC("minimax (cheat)", new MMAB(true)),
    MCTS("monte carlo tree search", new MCTSAI(false)),
    MCTSC("monte carlo tree search (cheat)", new MCTSAI(true)),
    VEGAS("vegas", new VegasAI(false)),
    VEGASC("vegas (cheat)", new VegasAI(true)),
    FIREMIND("firemind", new FiremindAI(false)),
    MMABFast("minimax (deck strength)", magic.ai.MMAB.DeckStrAI()),

    MCTS2("monte carlo tree search 2", new magic.ai.next.MCTSAI(false)),
    MCTSC2("monte carlo tree search 2 (cheat)", new magic.ai.next.MCTSAI(true)),
    ;

    public static final MagicAIImpl[] SUPPORTED_AIS = {MMAB, MMABC, MCTS, MCTSC, VEGAS, VEGASC, FIREMIND};

    private final String name;
    private final MagicAI ai;

    private MagicAIImpl(final String name, final MagicAI ai) {
        this.name=name;
        this.ai=ai;
    }

    public MagicAI getAI() {
        return ai;
    }

    @Override
    public String toString() {
        return name;
    }

}
