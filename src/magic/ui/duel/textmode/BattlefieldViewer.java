package magic.ui.duel.textmode;

import java.awt.BorderLayout;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import magic.ui.duel.SwingGameController;
import magic.ui.duel.PermanentFilter;
import magic.ui.duel.viewer.info.PermanentViewerInfo;
import magic.ui.widget.TitleBar;

@SuppressWarnings("serial")
class BattlefieldViewer extends PermanentsViewer {

    private final boolean opponent;
    private final PermanentFilter permanentFilter;

    BattlefieldViewer(final SwingGameController controller,final boolean opponent) {
        super(controller);
        this.opponent=opponent;
        permanentFilter=new PermanentFilter(this,controller);
        update();

        titleBar=new TitleBar("");
        add(titleBar,BorderLayout.NORTH);

        final JPanel filterPanel=new JPanel();
        permanentFilter.createFilterButtons(filterPanel,false);
        titleBar.add(filterPanel,BorderLayout.EAST);
    }

    @Override
    public String getTitle() {
        return "Battlefield : " + controller.getViewerInfo().getPlayerInfo(opponent).getName();
    }

    @Override
    public Collection<PermanentViewerInfo> getPermanents() {
        return permanentFilter.getPermanents(controller.getViewerInfo(), opponent);
    }

    @Override
    public boolean isSeparated(final PermanentViewerInfo permanentInfo1,final PermanentViewerInfo permanentInfo2) {
        return permanentInfo1.position!=permanentInfo2.position;
    }

    @Override
    public Border getBorder(final PermanentViewerInfo permanent) {
        return BorderFactory.createEmptyBorder();
    }
}
