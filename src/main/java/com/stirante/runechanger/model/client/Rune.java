package com.stirante.runechanger.model.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public enum Rune {
    //Generated on Friday, December 6, 2019 at 2:50:05 PM Central European Standard Time
    RUNE_8005(8005, 8000, 0, "Press the Attack"),
    RUNE_8008(8008, 8000, 0, "Lethal Tempo"),
    RUNE_8009(8009, 8000, 1, "Presence of Mind"),
    RUNE_8010(8010, 8000, 0, "Conqueror"),
    RUNE_8014(8014, 8000, 3, "Coup de Grace"),
    RUNE_8017(8017, 8000, 3, "Cut Down"),
    RUNE_8021(8021, 8000, 0, "Fleet Footwork"),
    RUNE_8105(8105, 8100, 3, "Relentless Hunter"),
    RUNE_8106(8106, 8100, 3, "Ultimate Hunter"),
    RUNE_8112(8112, 8100, 0, "Electrocute"),
    RUNE_8120(8120, 8100, 2, "Ghost Poro"),
    RUNE_8124(8124, 8100, 0, "Predator"),
    RUNE_8126(8126, 8100, 1, "Cheap Shot"),
    RUNE_8128(8128, 8100, 0, "Dark Harvest"),
    RUNE_8134(8134, 8100, 3, "Ingenious Hunter"),
    RUNE_8135(8135, 8100, 3, "Ravenous Hunter"),
    RUNE_8136(8136, 8100, 2, "Zombie Ward"),
    RUNE_8138(8138, 8100, 2, "Eyeball Collection"),
    RUNE_8139(8139, 8100, 1, "Taste of Blood"),
    RUNE_8143(8143, 8100, 1, "Sudden Impact"),
    RUNE_8210(8210, 8200, 2, "Transcendence"),
    RUNE_8214(8214, 8200, 0, "Summon Aery"),
    RUNE_8224(8224, 8200, 1, "Nullifying Orb"),
    RUNE_8226(8226, 8200, 1, "Manaflow Band"),
    RUNE_8229(8229, 8200, 0, "Arcane Comet"),
    RUNE_8230(8230, 8200, 0, "Phase Rush"),
    RUNE_8232(8232, 8200, 3, "Waterwalking"),
    RUNE_8233(8233, 8200, 2, "Absolute Focus"),
    RUNE_8234(8234, 8200, 2, "Celerity"),
    RUNE_8236(8236, 8200, 3, "Gathering Storm"),
    RUNE_8237(8237, 8200, 3, "Scorch"),
    RUNE_8242(8242, 8400, 3, "Unflinching"),
    RUNE_8275(8275, 8200, 1, "Nimbus Cloak"),
    RUNE_8299(8299, 8000, 3, "Last Stand"),
    RUNE_8304(8304, 8300, 1, "Magical Footwear"),
    RUNE_8306(8306, 8300, 1, "Hextech Flashtraption"),
    RUNE_8313(8313, 8300, 1, "Perfect Timing"),
    RUNE_8316(8316, 8300, 2, "Minion Dematerializer"),
    RUNE_8321(8321, 8300, 2, "Future's Market"),
    RUNE_8345(8345, 8300, 2, "Biscuit Delivery"),
    RUNE_8347(8347, 8300, 3, "Cosmic Insight"),
    RUNE_8351(8351, 8300, 0, "Glacial Augment"),
    RUNE_8352(8352, 8300, 3, "Time Warp Tonic"),
    RUNE_8358(8358, 8300, 0, "Prototype: Omnistone"),
    RUNE_8360(8360, 8300, 0, "Unsealed Spellbook"),
    RUNE_8401(8401, 8400, 1, "Shield Bash"),
    RUNE_8410(8410, 8300, 3, "Approach Velocity"),
    RUNE_8429(8429, 8400, 2, "Conditioning"),
    RUNE_8437(8437, 8400, 0, "Grasp of the Undying"),
    RUNE_8439(8439, 8400, 0, "Aftershock"),
    RUNE_8444(8444, 8400, 2, "Second Wind"),
    RUNE_8446(8446, 8400, 1, "Demolish"),
    RUNE_8451(8451, 8400, 3, "Overgrowth"),
    RUNE_8453(8453, 8400, 3, "Revitalize"),
    RUNE_8463(8463, 8400, 1, "Font of Life"),
    RUNE_8465(8465, 8400, 0, "Guardian"),
    RUNE_8473(8473, 8400, 2, "Bone Plating"),
    RUNE_9101(9101, 8000, 1, "Overheal"),
    RUNE_9103(9103, 8000, 2, "Legend: Bloodline"),
    RUNE_9104(9104, 8000, 2, "Legend: Alacrity"),
    RUNE_9105(9105, 8000, 2, "Legend: Tenacity"),
    RUNE_9111(9111, 8000, 1, "Triumph"),
    RUNE_9923(9923, 8100, 0, "Hail of Blades");


    private static final Logger log = LoggerFactory.getLogger(Rune.class);
    private final int id;
    private final Style style;
    private final int slot;
    private final String name;
    private BufferedImage image;

    Rune(int id, int styleId, int slot, String name) {
        this.id = id;
        style = Style.getById(styleId);
        this.slot = slot;
        this.name = name;
    }

    /**
     * Get rune by name
     *
     * @param name rune name
     * @return rune
     */
    public static Rune getByName(String name) {
        for (Rune rune : values()) {
            if (rune.name.equalsIgnoreCase(name) || rune.name.replaceAll("'", "’").equalsIgnoreCase(name)) {
                return rune;
            }
        }
        log.error("Rune name: " + name + " not found");
        return null;
    }

    /**
     * Get rune by id
     *
     * @param id rune id
     * @return rune
     */
    public static Rune getById(int id) {
        for (Rune rune : values()) {
            if (rune.id == id) {
                return rune;
            }
        }
        log.error("Rune id: " + id + " not found");
        return null;
    }

    /**
     * Get rune id
     *
     * @return rune id
     */
    public int getId() {
        return id;
    }

    /**
     * Get rune name
     *
     * @return rune name
     */
    public String getName() {
        return name;
    }

    /**
     * Get style this rune belongs to
     *
     * @return style
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Get slot where this rune can be placed
     *
     * @return slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Get rune image. Works only for keystones
     *
     * @return image
     */
    public BufferedImage getImage() {
        if (getSlot() != 0) {
            return null;
        }
        if (image == null) {
            try {
                image = ImageIO.read(getClass().getResourceAsStream("/runes/" + getId() + ".png"));
            } catch (IOException e) {
                log.error("Exception occurred while reading a rune icon", e);
            }
        }
        return image;
    }

    @Override
    public String toString() {
        return name() + "(" + name + ")";
    }
}
