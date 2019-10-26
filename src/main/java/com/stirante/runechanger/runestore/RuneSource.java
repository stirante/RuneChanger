package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.client.Champion;
import com.stirante.RuneChanger.model.client.RunePage;

import java.util.List;

public interface RuneSource {

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @return list of rune pages
     */
    List<RunePage> getForChampion(Champion champion);
}
