package magic.ui.duel.viewer;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import magic.model.phase.MagicPhaseType;
import magic.ui.utility.MagicStyle;
import magic.ui.duel.SwingGameController;
import magic.ui.duel.viewer.info.GameViewerInfo;
import magic.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class TurnStatusPanel extends JPanel {

    private final MigLayout miglayout = new MigLayout();
    private final TurnTitlePanel turnTitlePanel;
    private final PhaseStepViewer phaseStepViewer = new PhaseStepViewer();

    public TurnStatusPanel(final SwingGameController controller) {
        this.turnTitlePanel = new TurnTitlePanel(controller);
        setLookAndFeel();
        setLayout(miglayout);
        refreshLayout();
    }

    private void setLookAndFeel() {
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        setBackground(MagicStyle.getTheme().getColor(Theme.COLOR_TITLE_BACKGROUND));
        //
        phaseStepViewer.setOpaque(false);
    }

    private void refreshLayout() {
        miglayout.setLayoutConstraints("insets 3 2 2 2, gap 0, flowy");
        miglayout.setColumnConstraints("fill");
        removeAll();
        add(turnTitlePanel);
        add(phaseStepViewer, "aligny bottom, pushy");
    }

    public void refresh(final GameViewerInfo gameInfo, final MagicPhaseType phaseStep) {
        turnTitlePanel.refresh(gameInfo);
        phaseStepViewer.setPhaseStep(phaseStep);
    }

}
