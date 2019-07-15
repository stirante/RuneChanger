package com.stirante.RuneChanger.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Modifier {
    //from https://raw.communitydragon.org/pbe/plugins/rcp-be-lol-game-data/global/default/v1/perks.json
    RUNE_5001(5001, "+15-90 Health"),
    RUNE_5002(5002, "+6 Armor"),
    RUNE_5003(5003, "+8 Magic Resist"),
    RUNE_5005(5005, "+9% Attack Speed"),
    RUNE_5007(5007, "+1-10% CDR"),
    RUNE_5008(5008, "+9 Adaptive", "+5 AD or 9 AP");

    private final int id;
    private final String type;
    private final String onlineName;

    Modifier(int id, String type, String onlineName) {
        this.id = id;
        this.type = type;
        this.onlineName = onlineName;
    }

    Modifier(int id, String type) {
        this(id, type, type);
    }

    /**
     * Get modifier by name
     *
     * @param name modifier
     * @return modifier
     */
    public static Modifier getByName(String name) {
        for (Modifier mod : values()) {
            if (name.startsWith(mod.onlineName)) {
                return mod;
            }
        }
        log.error("Modifier name: " + name + " not found");
        return null;
    }

    /**
     * Get modifier by id
     *
     * @param id modifier
     * @return modifier
     */
    public static Modifier getById(Integer id) {
        for (Modifier mod : values()) {
            if (mod.id == id) {
                return mod;
            }
        }
        log.error("Modifier id: " + id + " not found");
        return null;
    }

    /**
     * Get modifier id
     *
     * @return modifier id
     */
    public int getId() {
        return id;
    }

    /**
     * Get modifier type name
     *
     * @return modifier type
     */
    public String getType() {
        return type;
    }

    /**
     * Get modifier name, used to parse online pages
     *
     * @return modifier online name
     */
    public String getOnlineName() {
        return onlineName;
    }

    @Override
    public String toString() {
        return name() + "(" + type + ")";
    }
}
