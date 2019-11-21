package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import javafx.collections.ObservableList;

import java.util.List;

public interface RuneSource {

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @param pages list of pages, which will be filled with pages
     */
    void getForChampion(Champion champion, ObservableList<RunePage> pages);

    /**
     * Returns friendly name of the source
     */
    String getSourceName();
}
