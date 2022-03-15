package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.utils.SyncingListWrapper;

public interface RuneSource extends Source {

    /**
     * Gets list of rune pages for champion
     *
     * @param data   data about the game
     * @param builds list of builds to fill
     */
    void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> builds);

    default GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.CLASSIC};
    }
}
