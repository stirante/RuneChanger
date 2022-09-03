package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.api.overlay.ClientOverlay;
import com.stirante.runechanger.api.overlay.OverlayLayer;
import com.stirante.runechanger.utils.Constants;
import com.stirante.runechanger.utils.SceneType;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.utils.ClassSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Set;

public class ClientOverlayImpl extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, ClientOverlay {
    private static final Logger log = LoggerFactory.getLogger(ClientOverlayImpl.class);

    /**
     * Application instance
     */
    final RuneChanger runeChanger;
    /**
     * Font used in client overlay
     */
    private Font font;
    /**
     * Current scene
     */
    private SceneType type;
    /**
     * Timer for rendering overlay
     */
    private final Timer timer;
    /**
     * Set of layers
     */
    private final Set<OverlayLayer> layers = new ClassSet<>(Comparator.comparingInt(OverlayLayer::getZIndex));

    private BufferedImage fake;
    private float lastX = 0f;
    private float lastY = 0f;

    public ClientOverlayImpl(RuneChanger runeChanger) {
        super();
        this.runeChanger = runeChanger;
        addLayer(new RuneMenu(this));
        addLayer(new QuickReplies(this));
        addLayer(new ChampionSuggestions(this));
        addLayer(new EstimatedPositions(this));
//        addLayer(new NotificationWindow(this));
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/Beaufort-Bold.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(12f);
            if (DebugConsts.DISPLAY_FAKE) {
                fake = ImageIO.read(new File("champ select.png"));
            }
        } catch (IOException | FontFormatException e) {
            log.error("Exception occurred while loading font", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading font", false);
        }
        setBackground(new Color(0f, 0f, 0f, 0f));
        timer = new Timer(16, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        timer.setRepeats(false);
    }

    @Override
    public void addLayer(OverlayLayer layer) {
        layers.add(layer);
    }

    @Override
    public <T extends OverlayLayer> T getLayer(Class<T> clz) {
        for (OverlayLayer layer : layers) {
            if (layer.getClass() == clz) {
                //noinspection unchecked
                return (T) layer;
            }
        }
        return null;
    }

    @Override
    public <T extends OverlayLayer> boolean removeLayer(Class<T> clz) {
        return layers.removeIf(layer -> layer.getClass() == clz);
    }

    @Override
    public void repaintLater() {
        if (timer != null) {
            timer.restart();
        }
    }

    @Override
    public RuneChangerApi getApi() {
        return runeChanger;
    }

    @Override
    public SceneType getSceneType() {
        return type;
    }

    @Override
    public void setSceneType(SceneType type) {
        this.type = type;
        timer.restart();
    }

    @Override
    public Font getDefaultFont() {
        return font;
    }

    @Override
    public void paintComponent(Graphics g) {
        int fontSize = (int) (Constants.FONT_SIZE * getHeight());
        if (font.getSize() != fontSize) {
            font = font.deriveFont((float) fontSize);
        }
        g.clearRect(0, 0, getWidth(), getHeight());

        //fake image with champion selection for quick layout checking
        if (DebugConsts.DISPLAY_FAKE) {
            g.drawImage(fake, 0, 0, null);
        }

        for (OverlayLayer layer : layers) {
            layer.drawOverlay(g);
        }

    }

    private int getClientWidth() {
        return (int) (getWidth() - (Constants.EXTRA_WIDTH * getHeight()));
    }

    public void clearRect(Graphics g, int x, int y, int w, int h) {
        ((Graphics2D) g).setComposite(AlphaComposite.Clear);
        g.fillRect(x, y, w, h);
        ((Graphics2D) g).setComposite(AlphaComposite.Src);
        if (DebugConsts.DISPLAY_FAKE) {
            g.drawImage(fake.getSubimage(x, y, w, Math.max(1, Math.min(fake.getHeight(), h))), x, y, null);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (DebugConsts.DISPLAY_FAKE) {
            float x = ((float) e.getX()) / ((float) (getClientWidth()));
            float y = ((float) e.getY()) / ((float) getHeight());
            if (e.getButton() == MouseEvent.BUTTON1) {
                lastX = x;
                lastY = y;
                log.debug("Left click event registered [x = " + x + " y = " + y + "]");
            }
            else {
                log.debug("Distance of click event: " + (x - lastX) + " x " + (y - lastY));
            }
        }
        for (OverlayLayer layer : layers) {
            layer.mouseReleased(e);
        }
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        for (OverlayLayer layer : layers) {
            layer.mouseClicked(mouseEvent);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        for (OverlayLayer layer : layers) {
            layer.mousePressed(mouseEvent);
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        for (OverlayLayer layer : layers) {
            layer.mouseEntered(mouseEvent);
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        for (OverlayLayer layer : layers) {
            layer.mouseDragged(mouseEvent);
        }
    }

    public void mouseMoved(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        for (OverlayLayer layer : layers) {
            layer.mouseMoved(e);
        }
    }

    public void mouseExited(MouseEvent e) {
        for (OverlayLayer layer : layers) {
            layer.mouseExited(e);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        for (OverlayLayer layer : layers) {
            layer.mouseWheelMoved(e);
        }
    }

    void initGraphics(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setFont(font);
    }
}
