package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.SyncingListWrapper;

public interface RuneSource extends Source {

    /**
     * Gets list of rune pages for champion
     *
     * @param data  data about the game
     * @param pages list of pages, which will be filled with pages
     */
    void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages);

    default GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.CLASSIC};
    }
}
