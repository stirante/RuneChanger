package com.stirante.runechanger.model.client;

public enum GameMap {
    MAP_1(1, "Summoner's Rift", "Original Summer variant"),
    MAP_2(2, "Summoner's Rift", "Original Autumn variant"),
    MAP_3(3, "The Proving Grounds", "Tutorial Map"),
    MAP_4(4, "Twisted Treeline", "Original Version"),
    MAP_8(8, "The Crystal Scar", "Dominion map"),
    MAP_10(10, "Twisted Treeline", "Last TT map"),
    MAP_11(11, "Summoner's Rift", "Current Version"),
    MAP_12(12, "Howling Abyss", "ARAM map"),
    MAP_14(14, "Butcher's Bridge", "Alternate ARAM map");

    private final int id;
    private final String mapName;
    private final String description;

    GameMap(int id, String mapName, String description) {
        this.id = id;
        this.mapName = mapName;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getMapName() {
        return mapName;
    }

    public String getDescription() {
        return description;
    }

    public static GameMap getById(int id) {
        for (GameMap map : values()) {
            if (map.getId() == id) {
                return map;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "GameMap{" +
                "id=" + id +
                ", mapName='" + mapName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
