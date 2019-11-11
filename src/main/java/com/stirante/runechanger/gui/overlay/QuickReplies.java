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
    private static final Color BG_COLOR = new Color(0f, 0f, 0f, 0.01f);

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
            int chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
            int chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
            for (int i = 0; i < messages.length; i++) {
                rectangles[i].x = chatX;
                rectangles[i].y = chatY;
                rectangles[i].width = 15;
                rectangles[i].height = 15;
                //we give it a background, since otherwise it wouldn't be clickable everywhere
                g2d.setPaint(BG_COLOR);
                g2d.fillRect(chatX, chatY, 15, 15);
                g2d.drawImage(icons[i], chatX, chatY, 15, 15, null);
                chatX += 25;
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        new Thread(() -> {
            for (int i = 0; i < rectangles.length; i++) {
                Rectangle rectangle = rectangles[i];
                if (rectangle.contains(e.getX(), e.getY())) {
                    getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(messages[i]);
                    return;
                }
            }
        }).start();
    }

    public void mouseMoved(MouseEvent e) {
        for (Rectangle rectangle : rectangles) {
            if (rectangle.contains(e.getX(), e.getY())) {
                getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
    }
}
