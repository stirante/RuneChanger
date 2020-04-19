package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import javafx.collections.ObservableList;

public interface RuneSource extends Source {

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @param pages    list of pages, which will be filled with pages
     */
    void getRunesForChampion(Champion champion, ObservableList<RunePage> pages);
}
