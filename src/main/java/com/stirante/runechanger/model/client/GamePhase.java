package com.stirante.runechanger.model.client;

public enum GamePhase {
    PLANNING,
    BAN_PICK,
    FINALIZATION,
    GAME_STARTING,
    NONE;

    public static GamePhase getByName(String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        for (GamePhase value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return NONE;
    }

}
