package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;
import com.stirante.runechanger.model.client.GameMap;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class QuickReplies extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(QuickReplies.class);
    private final BufferedImage[] icons = new BufferedImage[6];
    private final String[] messages = new String[]{
            LangHelper.getLang().getString("bot_msg"),
            LangHelper.getLang().getString("supp_msg"),
            LangHelper.getLang().getString("jungle_msg"),
            LangHelper.getLang().getString("mid_msg"),
            LangHelper.getLang().getString("top_msg")//,
//            ""
    };
    private final String[] messageKeys = new String[]{
            SimplePreferences.SettingsKeys.ADC_MESSAGE,
            SimplePreferences.SettingsKeys.SUPP_MESSAGE,
            SimplePreferences.SettingsKeys.JUNGLE_MESSAGE,
            SimplePreferences.SettingsKeys.MID_MESSAGE,
            SimplePreferences.SettingsKeys.TOP_MESSAGE,
            SimplePreferences.SettingsKeys.CUSTOM_MESSAGE_TEXT
    };
    private final Rectangle[] rectangles = new Rectangle[5];

    public QuickReplies(ClientOverlay overlay) {
        super(overlay);
        try {
            icons[0] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-bottom.png"));
            icons[1] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-utility.png"));
            icons[2] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-jungle.png"));
            icons[3] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-middle.png"));
            icons[4] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-top.png"));
            icons[5] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-top.png"));
        } catch (IOException e) {
            log.error("Exception occurred while loading position icons", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading position icons", false);
        }
        for (int i = 0; i < rectangles.length; i++) {
            rectangles[i] = new Rectangle(0, 0, 0, 0);
        }
    }

    @Override
    protected void draw(Graphics g) {
        if (getSceneType() == SceneType.CHAMPION_SELECT &&
                SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.QUICK_REPLIES, false)) {
            if (getRuneChanger().getChampionSelectionModule().isPositionSelector() ||
                    getRuneChanger().getChampionSelectionModule().getMap() != GameMap.MAP_11) {
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
                g2d.setPaint(DARKEN_COLOR);
                g2d.fillRect(chatX, chatY, 15, 15);
                g2d.drawImage(icons[i], chatX, chatY, 15, 15, null);
                chatX += 25;
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            for (int i = 0; i < rectangles.length; i++) {
                Rectangle rectangle = rectangles[i];
                if (rectangle.contains(e.getX(), e.getY())) {
                    String message = SimplePreferences.getStringValue(messageKeys[i], messages[i]);
                    if (message == null || message.isEmpty()) {
                        message = messages[i];
                    }
                    if (message != null && !message.isEmpty()) {
                        getRuneChanger().getChampionSelectionModule().sendMessageToChampSelect(message);
                    }
                    return;
                }
            }
        });
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
