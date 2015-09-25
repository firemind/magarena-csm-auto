package magic.ui.screen.widget;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import magic.data.MagicIcon;
import magic.ui.IconImages;
import magic.ui.utility.GraphicsUtils;

@SuppressWarnings("serial")
public class DialButton extends ActionBarButton {

    private static final ImageIcon DIAL_ICON = IconImages.getIcon(MagicIcon.MARKER_ICON);

    private final int increment;
    private int position;

    public DialButton(int count, int start, String caption, String tooltip, AbstractAction action) {

        super(DIAL_ICON, caption, tooltip, action);

        increment = 360 / count;
        position = increment * start;
        
        rotateIconImage();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                position = (position + increment) % 360;
                rotateIconImage();
            }
        });
    }

    private void rotateIconImage() {
        final BufferedImage image = GraphicsUtils.getCompatibleBufferedImage(
            DIAL_ICON.getIconWidth(), DIAL_ICON.getIconWidth(), BufferedImage.TRANSLUCENT
        );
        final Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.rotate(Math.toRadians(position), image.getWidth() / 2.0D, image.getHeight() / 2.0D);
        g2d.drawImage(DIAL_ICON.getImage(), 0, 0, null);
        super.setIcon(new ImageIcon(image));
        g2d.dispose();
    }

}
