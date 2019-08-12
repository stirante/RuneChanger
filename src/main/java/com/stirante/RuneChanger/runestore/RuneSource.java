package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;

import java.util.List;

public interface RuneSource {

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @return list of rune pages
     */
    List<RunePage> getForChampion(Champion champion);

    String getSourceName();
}
