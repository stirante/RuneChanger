package com.stirante.runechanger.model.client;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.utils.PathUtils;
import com.stirante.runechanger.utils.SwingUtils;
import generated.Position;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChampionImpl implements Champion {

    private static final File portraitsDir = new File(PathUtils.getAssetsDir(), "champions");
    private static final LoadingCache<ChampionImpl, java.awt.Image> portraitCache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener((key, value, removalCause) -> {
                if (value != null) {
                    ((java.awt.Image) value).flush();
                }
            })
            .build(key -> {
                BufferedImage img;
                try {
                    img = ImageIO.read(new File(portraitsDir, key.getId() + ".jpg"));
                } catch (Exception e) {
                    return null;
                }
                if (img == null) {
                    return null;
                }
                BufferedImage scaledImage = SwingUtils.getScaledImage(70, 70, img);
                img.flush();
                return scaledImage;
            });

    private final int id;
    private String internalName;
    private String name;
    private String alias;
    private String pickQuote = "";
    private Position position;

    public ChampionImpl(int id, String internalName, String name, String alias) {
        this.id = id;
        this.internalName = internalName;
        this.name = name;
        this.alias = alias;
    }

    @Override
    public java.awt.Image getPortrait() {
        return portraitCache.get(this);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getPickQuote() {
        return pickQuote;
    }

    @Override
    public BufferedImage getSplashArt() {
        try {
            return ImageIO.read(new File(portraitsDir, id + "_full.jpg"));
        } catch (IOException e) {
            return null;
        }
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setPickQuote(String pickQuote) {
        this.pickQuote = pickQuote;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChampionImpl && ((ChampionImpl) obj).id == id;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name, id);
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

}
