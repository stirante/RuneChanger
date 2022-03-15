package com.stirante.runechanger.api.overlay;

import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.utils.SceneType;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;

public interface ClientOverlay {

    void addLayer(OverlayLayer layer);

    <T extends OverlayLayer> T getLayer(Class<T> clz);

    <T extends OverlayLayer> boolean removeLayer(Class<T> clz);

    SceneType getSceneType();

    void setSceneType(SceneType type);

    Font getDefaultFont();

    void repaint();

    void repaintLater();

    int getWidth();

    int getHeight();

    RuneChangerApi getApi();

    void clearRect(Graphics g, int x, int y, int w, int h);

    public void setCursor(Cursor cursor);

}
