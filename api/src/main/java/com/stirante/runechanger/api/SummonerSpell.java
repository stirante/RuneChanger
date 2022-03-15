package com.stirante.runechanger.api;

public enum SummonerSpell {
    BARRIER("SummonerBarrier", 21, "Barrier"),
    CLEANSE("SummonerBoost", 1, "Cleanse"),
    IGNITE("SummonerDot", 14, "Ignite"),
    EXHAUST("SummonerExhaust", 3, "Exhaust"),
    FLASH("SummonerFlash", 4, "Flash"),
    GHOST("SummonerHaste", 6, "Ghost"),
    HEAL("SummonerHeal", 7, "Heal"),
    CLARITY("SummonerMana", 13, "Clarity"),
    TO_THE_KING("SummonerPoroRecall", 30, "To the King!"),
    PORO_TOSS("SummonerPoroThrow", 31, "Poro Toss"),
    SMITE("SummonerSmite", 11, "Smite"),
    MARK_URF("SummonerSnowURFSnowball_Mark", 39, "Mark"),
    MARK("SummonerSnowball", 32, "Mark"),
    TELEPORT("SummonerTeleport", 12, "Teleport");

    private final String id;
    private final int key;
    private final String name;

    SummonerSpell(String id, int key, String name) {
        this.id = id;
        this.key = key;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public int getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public static SummonerSpell getByKey(int key) {
        for (SummonerSpell v : values()) {
            if (v.key == key) {
                return v;
            }
        }
        return null;
    }

    public static SummonerSpell getByName(String name) {
        for (SummonerSpell v : values()) {
            if (v.name.equalsIgnoreCase(name) || v.id.equalsIgnoreCase(name)) {
                return v;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SummonerSpell{" +
                "id='" + id + '\'' +
                ", key=" + key +
                ", name='" + name + '\'' +
                '}';
    }
}
