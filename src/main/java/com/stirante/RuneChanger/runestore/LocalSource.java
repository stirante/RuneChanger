package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.util.SimplePreferences;

import java.util.List;

public class LocalSource implements RuneSource {
    @Override
    public List<RunePage> getForChampion(Champion champion) {
        return SimplePreferences.getRuneBookValues();
    }
}
