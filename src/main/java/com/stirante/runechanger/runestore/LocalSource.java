package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.client.Champion;
import com.stirante.RuneChanger.model.client.RunePage;
import com.stirante.RuneChanger.util.SimplePreferences;

import java.util.List;

public class LocalSource implements RuneSource {
    @Override
    public List<RunePage> getForChampion(Champion champion) {
        return SimplePreferences.getRuneBookValues();
    }

    @Override
    public String getSourceName() {
        return "Local";
    }
}
