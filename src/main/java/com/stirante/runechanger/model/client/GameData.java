package com.stirante.runechanger.model.client;

public class GameData {

    private final GameMode mode;
    private final Champion champion;

    private GameData(Champion champion, GameMode mode) {
        this.mode = mode;
        this.champion = champion;
    }

    public static GameData of(Champion champion, GameMode mode) {
        if (champion == null) {
            throw new IllegalArgumentException("Champion cannot be null!");
        }
        return new GameData(champion, mode);
    }

    public static GameData of(Champion champion) {
        return of(champion, GameMode.CLASSIC);
    }

    public Champion getChampion() {
        return champion;
    }

    public GameMode getGameMode() {
        return mode;
    }
}
