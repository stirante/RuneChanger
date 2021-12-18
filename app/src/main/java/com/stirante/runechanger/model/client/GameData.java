package com.stirante.runechanger.model.client;

public class GameData {

    private final GameMode mode;
    private final Champion champion;
    private Context context = Context.UNKNOWN;

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

    public GameData addContext(Context context) {
        this.context = context;
        return this;
    }

    public Champion getChampion() {
        return champion;
    }

    public GameMode getGameMode() {
        return mode;
    }

    public Context getContext() {
        return this.context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GameData gameData = (GameData) o;

        if (mode != gameData.mode) {
            return false;
        }
        if (!champion.equals(gameData.champion)) {
            return false;
        }
        return context == gameData.context;
    }

    @Override
    public int hashCode() {
        int result = mode != null ? mode.hashCode() : 0;
        result = 31 * result + champion.hashCode();
        result = 31 * result + context.hashCode();
        return result;
    }

    public enum Context {
        GUI,
        CHAMPION_SELECT,
        UNKNOWN
    }
}
