package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.overlay.OverlayLayer;
import com.stirante.runechanger.model.client.GameMap;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.utils.AsyncTask;
import com.stirante.runechanger.utils.Constants;
import com.stirante.runechanger.utils.SceneType;
import com.stirante.runechanger.utils.SimplePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class QuickReplies extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(QuickReplies.class);
    private final BufferedImage[] icons = new BufferedImage[6];
    private final String[] messages = new String[]{
            "bot",
            "supp",
            "jungle",
            "mid",
            "top",
            //Must be empty, it's placeholder for custom message
            ""
    };
    private final String[] messageKeys = new String[]{
            SimplePreferences.SettingsKeys.ADC_MESSAGE,
            SimplePreferences.SettingsKeys.SUPP_MESSAGE,
            SimplePreferences.SettingsKeys.JUNGLE_MESSAGE,
            SimplePreferences.SettingsKeys.MID_MESSAGE,
            SimplePreferences.SettingsKeys.TOP_MESSAGE,
            SimplePreferences.SettingsKeys.CUSTOM_MESSAGE_TEXT
    };
    private final Rectangle[] rectangles = new Rectangle[6];

    public QuickReplies(ClientOverlayImpl overlay) {
        super(overlay);
        try {
            icons[0] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-bottom.png"));
            icons[1] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-utility.png"));
            icons[2] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-jungle.png"));
            icons[3] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-middle.png"));
            icons[4] = ImageIO.read(getClass().getResourceAsStream("/images/icon-position-top.png"));
            icons[5] = ImageIO.read(getClass().getResourceAsStream("/images/send.png"));
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
        //TODO: This needs MAJOR refactoring, please someone do this for me
        if (getSceneType() == SceneType.CHAMPION_SELECT) {
            String customMessage =
                    SimplePreferences.getStringValue(SimplePreferences.SettingsKeys.CUSTOM_MESSAGE_TEXT, "");
            boolean isQuickReplies =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.QUICK_REPLIES, false);
            boolean isForceQuickReplies =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.FORCE_QUICK_REPLIES, false);
            boolean isCustomMessage =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.CUSTOM_MESSAGE, false) &&
                            customMessage != null && !customMessage.isEmpty();
            if (isQuickReplies || isCustomMessage) {
                boolean onlyCustomMessage = isCustomMessage && !isQuickReplies;
                if ((((RuneChanger) getApi()).getChampionSelectionModule().isPositionSelector() ||
                        ((RuneChanger) getApi()).getChampionSelectionModule().getMap() != GameMap.MAP_11) && !isForceQuickReplies) {
                    if (isCustomMessage) {
                        onlyCustomMessage = true;
                    }
                    else {
                        return;
                    }
                }
                Graphics2D g2d = (Graphics2D) g;
                int chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
                int chatX = (int) (Constants.QUICK_CHAT_X * getClientWidth());
                for (int i = onlyCustomMessage ? 5 : 0;
                     i < (isCustomMessage ? messages.length : messages.length - 1); i++) {
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
    }

    public void mouseReleased(MouseEvent e) {
        //TODO: This needs MAJOR refactoring, please someone do this for me
        AsyncTask.EXECUTOR_SERVICE.submit(() -> {
            String customMessage =
                    SimplePreferences.getStringValue(SimplePreferences.SettingsKeys.CUSTOM_MESSAGE_TEXT, "");
            boolean isQuickReplies =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.QUICK_REPLIES, false);
            boolean isForceQuickReplies =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.FORCE_QUICK_REPLIES, false);
            boolean isCustomMessage =
                    SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.CUSTOM_MESSAGE, false) &&
                            customMessage != null && !customMessage.isEmpty();
            boolean onlyCustomMessage = isCustomMessage && !isQuickReplies;
            if ((((RuneChanger) getApi()).getChampionSelectionModule().isPositionSelector() ||
                    ((RuneChanger) getApi()).getChampionSelectionModule().getMap() != GameMap.MAP_11) && !isForceQuickReplies) {
                if (isCustomMessage) {
                    onlyCustomMessage = true;
                }
                else {
                    return;
                }
            }
            for (int i = onlyCustomMessage ? 5 : 0;
                 i < (isCustomMessage ? rectangles.length : rectangles.length - 1); i++) {
                Rectangle rectangle = rectangles[i];
                if (rectangle.contains(e.getX(), e.getY())) {
                    String message = SimplePreferences.getStringValue(messageKeys[i], messages[i]);
                    if (message == null || message.isEmpty()) {
                        message = messages[i];
                    }
                    if (message != null && !message.isEmpty()) {
                        ((RuneChanger) getApi()).getChampionSelectionModule().sendMessageToChampSelect(message);
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
