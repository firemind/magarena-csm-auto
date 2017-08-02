package magic.ui.screen;

import java.util.List;
import javax.swing.SwingWorker;
import magic.translate.MText;
import magic.utility.MagicSystem;

class CardsLoadingWorker extends SwingWorker<Void, String> {

    // translatable strings
    private static final String _S1 = "loading card data";

    private final CardsLoadingPanel statusPanel;

    private final boolean needsPlayableCards;
    private final boolean needsMissingCards;

    CardsLoadingWorker(CardsLoadingPanel p) {
        this.statusPanel = p;
        needsPlayableCards = Boolean.valueOf(p.needsPlayableCards());
        needsMissingCards = Boolean.valueOf(p.needsMissingCards());
    }

    @Override
    protected Void doInBackground() throws Exception {
        if (needsPlayableCards || needsMissingCards) {
            publish(MText.get(_S1));
        }
        if (needsPlayableCards) {
            MagicSystem.waitForPlayableCards();
        }
        if (needsMissingCards) {
            MagicSystem.waitForMissingCards();
        }
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        statusPanel.setMessage("..." + chunks.get(0) + "...");
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            statusPanel.getRunnable().run();
        }
    }

}
