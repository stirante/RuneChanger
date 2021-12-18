package com.stirante.runechanger.model.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Modifier {
    //from https://raw.communitydragon.org/pbe/plugins/rcp-be-lol-game-data/global/default/v1/perks.json
    RUNE_5001(5001, "+15-140 Health", 2),
    RUNE_5002(5002, "+6 Armor", 1, 2),
    RUNE_5003(5003, "+8 Magic Resist", 1, 2),
    RUNE_5005(5005, "+10% Attack Speed", 0),
    RUNE_5007(5007, "+1-10% CDR", 0),
    RUNE_5008(5008, "+9 Adaptive", "+5 AD or 9 AP", 0, 1);

    private static final Logger log = LoggerFactory.getLogger(Modifier.class);
    private final int id;
    private final String type;
    private final String onlineName;
    private final List<Integer> slots;

    Modifier(int id, String type, String onlineName, int... slots) {
        this.id = id;
        this.type = type;
        this.onlineName = onlineName;
        this.slots = Arrays.stream(slots).boxed().collect(Collectors.toList());
    }

    Modifier(int id, String type, int... slots) {
        this(id, type, type, slots);
    }

    public List<Integer> getSlots() {
        return slots;
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
