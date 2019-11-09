package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;
import com.stirante.runechanger.util.SimplePreferences;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class QuickReplies extends OverlayLayer {
    public static final Color BG_COLOR = new Color(0f, 0f, 0f, 0.01f);
    //    private static final String BOT = "BOT";
//    private static final String MID = "MID";
//    private static final String TOP = "TOP";
//    private static final String JUNGLE = "JUNGLE";
//
//    private final Rectangle botButton = new Rectangle(0, 0, 0, 0);
//    private final Rectangle midButton = new Rectangle(0, 0, 0, 0);
//    private final Rectangle topButton = new Rectangle(0, 0, 0, 0);
//    private final Rectangle jungleButton = new Rectangle(0, 0, 0, 0);
//
//    private BufferedImage botIcon;
//    private BufferedImage midIcon;
//    private BufferedImage topIcon;
//    private BufferedImage jungleIcon;

    private BufferedImage[] icons = new BufferedImage[5];
    private String[] messages = new String[]{"bot", "support", "jungle", "mid", "top"};
    private Rectangle[] rectangles = new Rectangle[5];

    public QuickReplies(ClientOverlay overlay) {
        super(overlay);
        try {
            icons[0] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-bottom.png"));
            icons[1] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-utility.png"));
            icons[2] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-jungle.png"));
            icons[3] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-middle.png"));
            icons[4] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-top.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < rectangles.length; i++) {
            rectangles[i] = new Rectangle(0, 0, 0, 0);
        }
    }

    @Override
    protected void draw(Graphics g) {
        if (getSceneType() == SceneType.CHAMPION_SELECT &&
                Boolean.parseBoolean(SimplePreferences.getValue(SimplePreferences.SettingsKeys.QUICK_REPLIES))) {
            if (getRuneChanger().getChampionSelectionModule().isPositionSelector()) {
                return;
            }
            Graphics2D g2d = (Graphics2D) g;
            drawV2(g2d);
//            int chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
//            int chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
//            g2d.rotate(Math.toRadians(-90), chatX, chatY);
//
//            int botWidth = g2d.getFontMetrics().stringWidth(BOT);
//            int midWidth = g2d.getFontMetrics().stringWidth(MID);
//            int topWidth = g2d.getFontMetrics().stringWidth(TOP);
//            int bgHeight = g2d.getFontMetrics().getHeight() - (g2d.getFontMetrics().getAscent() / 2);
//
//            g2d.setColor(DARKEN_COLOR);
//            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
//                    (g2d.getFontMetrics().getAscent() / 2), botWidth, bgHeight);
//            botButton.x =
//                    (int) (Constants.QUICK_CHAT_X * (getClientWidth())) - g2d.getFontMetrics().getHeight() +
//                            (g2d.getFontMetrics().getAscent() / 2);
//            botButton.y = chatY - botWidth;
//            chatX = drawButton(g2d, chatX, chatY, bgHeight, BOT, botWidth, midWidth, botButton, midButton);
//            midButton.y = chatY - botWidth - Constants.MARGIN - midWidth;
//            chatX = drawButton(g2d, chatX, chatY, bgHeight, MID, midWidth, topWidth, midButton, topButton);
//            topButton.y = chatY - botWidth - Constants.MARGIN - midWidth - Constants.MARGIN - topWidth;
//            drawButton(g2d, chatX, chatY, bgHeight, TOP, topWidth, 0, topButton, null);
//
//            chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
//            chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
//            g2d.rotate(Math.toRadians(90), chatX, chatY);
        }
    }

    private void drawV2(Graphics2D g2d) {
        int chatY = (int) (0.97083336f * getHeight());
        int chatX = (int) (0.0171875f * getClientWidth());
        for (int i = 0; i < messages.length; i++) {
            rectangles[i].x = chatX;
            rectangles[i].y = chatY;
            rectangles[i].width = 15;
            rectangles[i].height = 15;
            //we give it a background, since otherwise it wouldn't be clickable everywhere
            g2d.setPaint(BG_COLOR);
            g2d.fillRect(chatX, chatY, 20, 20);
            g2d.drawImage(icons[i], chatX, chatY, 20, 20, null);
            chatX += 25;
        }
    }

//    @SuppressWarnings("SuspiciousNameCombination")
//    private int drawButton(Graphics2D g2d, int chatX, int chatY, int bgHeight, String text, int width, int nextWidth, Rectangle button, Rectangle nextButton) {
//        button.width = bgHeight;
//        button.height = width;
//        g2d.setColor(TEXT_COLOR);
//        g2d.drawString(text, chatX, chatY);
//        chatX += width + Constants.MARGIN;
//
//        g2d.setColor(DARKEN_COLOR);
//        g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() +
//                (g2d.getFontMetrics().getAscent() / 2), nextWidth, bgHeight);
//        if (nextButton != null) {
//            nextButton.x = button.x;
//        }
//        return chatX;
//    }

    public void mouseReleased(MouseEvent e) {
        new Thread(() -> {
            for (int i = 0; i < rectangles.length; i++) {
                Rectangle rectangle = rectangles[i];
                if (rectangle.contains(e.getX(), e.getY())) {
                    getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(messages[i]);
                    return;
                }
            }
//            if (botButton.contains(e.getX(), e.getY())) {
//                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(BOT.toLowerCase());
//            }
//            else if (midButton.contains(e.getX(), e.getY())) {
//                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(MID.toLowerCase());
//            }
//            else if (topButton.contains(e.getX(), e.getY())) {
//                getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(TOP.toLowerCase());
//            }
        }).start();
    }

    public void mouseMoved(MouseEvent e) {
        for (Rectangle rectangle : rectangles) {
            if (rectangle.contains(e.getX(), e.getY())) {
                getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
//        if (botButton.contains(e.getX(), e.getY()) ||
//                midButton.contains(e.getX(), e.getY()) ||
//                topButton.contains(e.getX(), e.getY())) {
//            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        }
    }
}
