package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.RunePage;
import javafx.collections.ObservableList;

public interface RuneSource extends Source {

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @param mode     current game mode
     * @param pages    list of pages, which will be filled with pages
     */
    void getRunesForChampion(Champion champion, GameMode mode, ObservableList<RunePage> pages);

    default boolean hasGameModeSpecific() {
        return false;
    }
}
