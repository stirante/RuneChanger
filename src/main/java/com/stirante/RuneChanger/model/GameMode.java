package com.stirante.RuneChanger.model;

public enum GameMode {
    ARAM("ARAM"),
    ASCENSION("Ascension"),
    CLASSIC("Classic"),
    FIRSTBLOOD("Showdown"),
    KINGPORO("Poro King"),
    ODIN("Dominion"),
    ONEFORALL("One For All"),
    TUTORIAL("Tutorial"),
    SIEGE("Nexus Siege"),
    ASSASSINATE("Assassinate"),
    DARKSTAR("Dark Star"),
    ARSR("All Random Summoner's Rift"),
    URF("Urf"),
    DOOMBOTSTEEMO("Doom Bots"),
    STARGUARDIAN("Star Guardian"),
    PROJECT("Project"),
    OVERCHARGE("Overcharge"),
    SNOWURF("All Random Urf Snow"),
    GAMEMODEX("Nexus Blitz"),
    ODYSSEY("Odyssey: Extraction"),
    TUTORIAL_MODULE_1("Tutorial Part 1"),
    TUTORIAL_MODULE_2("Tutorial Part 2"),
    TUTORIAL_MODULE_3("Tutorial Part 3");

    private final String name;

    GameMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
