package com.stirante.runechanger.gui.overlay;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.utils.SwingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.LineMetrics;
import java.io.IOException;
import java.util.Objects;

public class NotificationWindow extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(NotificationWindow.class);
    private Image warnIcon;
    private Image closeIcon;
    private final Rectangle warningCloseButton = new Rectangle(0, 0, 0, 0);
    private boolean warningVisible = false;
    private String message;

    public NotificationWindow(ClientOverlay overlay) {
        super(overlay);
        try {
            warnIcon =
                    SwingUtils.getScaledImage(32, 32, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/info-yellow.png"))));
            closeIcon =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/close.png"))));
        } catch (IOException e) {
            log.error("Exception occurred while loading notification icons", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading notification icons", false);
        }
        EventBus.register(this);
    }

    @Subscribe(NotificationMessageEvent.NAME)
    public void onNotificationMessage(NotificationMessageEvent e) {
        warningVisible = true;
        message = e.getMessage();
    }

    @Override
    protected void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (!warningVisible || message == null) {
            return;
        }
        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(15f));

        LineMetrics metrics = g2d.getFontMetrics().getLineMetrics(message, g2d);
        int border = 2;
        int margin = 5;
        int height = 50;
        int width = (2 * border) + (4 * margin) + warnIcon.getWidth(null) + g2d.getFontMetrics().stringWidth(message) +
                closeIcon.getWidth(null);
        int x = (int) ((getClientWidth() * Constants.WARNING_X) - (width / 2));
        int y = (int) (getHeight() * Constants.WARNING_Y);

        g2d.setPaint(new GradientPaint(0, y, GRADIENT_COLOR_1, 0, y + height, GRADIENT_COLOR_2));
        g2d.fillRect(x, y, width, height);
        g2d.setPaint(BACKGROUND_COLOR);
        g2d.fillRect(x + border, y + border, width - (2 * border), height - (2 * border));
        g2d.setPaint(TEXT_COLOR);
        g2d.drawImage(
                closeIcon,
                (x + width - (border * 2)) - closeIcon.getWidth(null),
                y + border,
                null
        );
        warningCloseButton.setLocation((x + width - (border * 2)) - closeIcon.getWidth(null), y + border);
        warningCloseButton.setSize(closeIcon.getWidth(null), closeIcon.getHeight(null));
        g2d.drawImage(
                warnIcon,
                x + border + margin,
                y + (height / 2) - (warnIcon.getHeight(null) / 2),
                null
        );
        g2d.drawString(
                message,
                x + border + (margin * 2) + warnIcon.getWidth(null),
                y + (height / 2) + (int) (metrics.getAscent() / 4)
        );

        g2d.setFont(oldFont);
    }

    public void mouseReleased(MouseEvent e) {
        if (warningVisible && warningCloseButton.getSize().height > 0 &&
                warningCloseButton.contains(e.getX(), e.getY())) {
            warningVisible = false;
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (warningVisible && warningCloseButton.getSize().height > 0 &&
                warningCloseButton.contains(e.getX(), e.getY())) {
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    public static class NotificationMessageEvent {
        public static final String NAME = "NotificationMessageEvent";
        private final String message;

        public NotificationMessageEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
