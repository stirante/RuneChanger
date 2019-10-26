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
            chatX = drawButton(g2d, chatX, chatY, bgHeight, BOT, botWidth, midWidth, botButton, midButton);
            midButton.y = chatY - botWidth - Constants.MARGIN - midWidth;
            chatX = drawButton(g2d, chatX, chatY, bgHeight, MID, midWidth, topWidth, midButton, topButton);
            topButton.y = chatY - botWidth - Constants.MARGIN - midWidth - Constants.MARGIN - topWidth;
            drawButton(g2d, chatX, chatY, bgHeight, TOP, topWidth, 0, topButton, null);

            chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
            chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
            g2d.rotate(Math.toRadians(90), chatX, chatY);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private int drawButton(Graphics2D g2d, int chatX, int chatY, int bgHeight, String text, int width, int nextWidth, Rectangle button, Rectangle nextButton) {
        button.width = bgHeight;
        button.height = width;
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, chatX, chatY);
        chatX += width + Constants.MARGIN;

        g2d.setColor(DARKEN_COLOR);
        g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
                (g2d.getFontMetrics().getAscent() / 2), nextWidth, bgHeight);
        if (nextButton != null) {
            nextButton.x = button.x;
        }
        return chatX;
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
