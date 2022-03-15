package com.stirante.runechanger.api;

import generated.Position;
import java.awt.image.BufferedImage;

public interface Champion {

    /**
     * Get champion's portrait
     */
    java.awt.Image getPortrait();

    /**
     * Get champion's id
     *
     * @return champion id
     */
    int getId();

    /**
     * Get riot internal champion name
     *
     * @return internal champion name
     */
    String getInternalName();

    /**
     * Get champion's name
     *
     * @return champion name
     */
    String getName();

    /**
     * Get alternative champion's name
     *
     * @return alias
     */
    String getAlias();

    /**
     * Get champion's pick quote
     *
     * @return alias
     */
    String getPickQuote();

    /**
     * Get champion's splash art
     */
    BufferedImage getSplashArt();

    /**
     * Get champion's role
     */
    Position getPosition();

}
