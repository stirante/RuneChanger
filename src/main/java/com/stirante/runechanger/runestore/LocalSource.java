package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.SimplePreferences;

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
