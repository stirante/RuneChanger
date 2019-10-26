package com.stirante.RuneChanger.gui.overlay;

import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.util.SimplePreferences;

import java.awt.*;
import java.awt.event.MouseEvent;

public class QuickReplies extends OverlayLayer {
    private static final String BOT = "BOT";
    private static final String MID = "MID";
    private static final String TOP = "TOP";

    private final Rectangle botButton = new Rectangle(0, 0, 0, 0);
    private final Rectangle midButton = new Rectangle(0, 0, 0, 0);
    private final Rectangle topButton = new Rectangle(0, 0, 0, 0);

    public QuickReplies(ClientOverlay overlay) {
        super(overlay);
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    protected void draw(Graphics g) {
        if (Boolean.parseBoolean(SimplePreferences.getSettingsValue("quickReplies"))) {
            if (getRuneChanger().getChampionSelectionModule().isPositionSelector()) {
                return;
            }
            Graphics2D g2d = (Graphics2D) g;
            int chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
            int chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
            g2d.rotate(Math.toRadians(-90), chatX, chatY);

            int botWidth = g2d.getFontMetrics().stringWidth(BOT);
            int midWidth = g2d.getFontMetrics().stringWidth(MID);
            int topWidth = g2d.getFontMetrics().stringWidth(TOP);
            int bgHeight = g2d.getFontMetrics().getHeight() - (g2d.getFontMetrics().getAscent() / 2);

            g2d.setColor(DARKEN_COLOR);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
                    (g2d.getFontMetrics().getAscent() / 2), botWidth, bgHeight);
            botButton.x =
                    (int) (Constants.QUICK_CHAT_X * (getClientWidth())) - g2d.getFontMetrics().getHeight() +
                            (g2d.getFontMetrics().getAscent() / 2);
            botButton.y = chatY - botWidth;
            botButton.width = bgHeight;
            botButton.height = botWidth;
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(BOT, chatX, chatY);
            chatX += botWidth + Constants.MARGIN;

            g2d.setColor(DARKEN_COLOR);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
                    (g2d.getFontMetrics().getAscent() / 2), midWidth, bgHeight);
            midButton.x = botButton.x;
            midButton.y = chatY - botWidth - Constants.MARGIN - midWidth;
            midButton.width = bgHeight;
            midButton.height = midWidth;
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(MID, chatX, chatY);
            chatX += midWidth + Constants.MARGIN;

            g2d.setColor(DARKEN_COLOR);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
                    (g2d.getFontMetrics().getAscent() / 2), topWidth, bgHeight);
            topButton.x = botButton.x;
            topButton.y = chatY - botWidth - Constants.MARGIN - midWidth - Constants.MARGIN - topWidth;
            topButton.width = bgHeight;
            topButton.height = topWidth;
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(TOP, chatX, chatY);

            chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
            chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
            g2d.rotate(Math.toRadians(90), chatX, chatY);
        }
    }

    public void mouseReleased(MouseEvent e) {
        new Thread(() -> {
            if (botButton.contains(e.getX(), e.getY())) {
                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(BOT.toLowerCase());
            }
            else if (midButton.contains(e.getX(), e.getY())) {
                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(MID.toLowerCase());
            }
            else if (topButton.contains(e.getX(), e.getY())) {
                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(TOP.toLowerCase());
            }
        }).start();
    }

    public void mouseMoved(MouseEvent e) {
        if (botButton.contains(e.getX(), e.getY()) ||
                midButton.contains(e.getX(), e.getY()) ||
                topButton.contains(e.getX(), e.getY())) {
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }
}
