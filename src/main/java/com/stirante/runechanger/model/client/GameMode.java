package com.stirante.runechanger.model.client;

public enum GameMode {
    ARAM("ARAM", false),
    ASCENSION("Ascension", true),
    CLASSIC("Classic", true),
    FIRSTBLOOD("Showdown", true),
    KINGPORO("Poro King", true),
    ODIN("Dominion", true),
    ONEFORALL("One For All", true),
    TUTORIAL("Tutorial", false),
    SIEGE("Nexus Siege", true),
    ASSASSINATE("Assassinate", true),
    DARKSTAR("Dark Star", false),
    ARSR("All Random Summoner's Rift", false),
    URF("Urf", true),
    DOOMBOTSTEEMO("Doom Bots", true),
    STARGUARDIAN("Star Guardian", true),
    PROJECT("Project", true),
    OVERCHARGE("Overcharge", true),
    SNOWURF("All Random Urf Snow", false),
    GAMEMODEX("Nexus Blitz", true),
    ODYSSEY("Odyssey: Extraction", true),
    TUTORIAL_MODULE_1("Tutorial Part 1", false),
    TUTORIAL_MODULE_2("Tutorial Part 2", false),
    TUTORIAL_MODULE_3("Tutorial Part 3", false),
    TFT("Team Fight Tactics", false, true),
    NEXUSBLITZ("Nexus Blitz", true, false),
    PRACTICETOOL("Practice tool", true),
    ULTBOOK("Ultimate Spellbook", true);

    private final String name;
    private final boolean championSelection;
    private final boolean disabled;

    GameMode(String name, boolean championSelection) {
        this(name, championSelection, false);
    }

    GameMode(String name, boolean championSelection, boolean disabled) {
        this.name = name;
        this.championSelection = championSelection;
        this.disabled = disabled;
    }

    public String getName() {
        return name;
    }

    public boolean hasChampionSelection() {
        return championSelection;
    }

    public boolean isDisabled() {
        return disabled;
    }
}
