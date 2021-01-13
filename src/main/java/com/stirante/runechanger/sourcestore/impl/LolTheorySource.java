package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.SyncingListWrapper;
import generated.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class LolTheorySource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(LolTheorySource.class);
    final String CHAMPION_URL = "https://loltheory-1b4da.firebaseio.com/%s/champions/%s.json";
    final String PATCH_URL = "https://loltheory-1b4da.firebaseio.com/%s.json?shallow=true";
    final String PUBLIC_URL = "https://loltheory.gg/champion/%s";
    private String patchString;

    @Override
    public String getSourceName() {
        return "LoLTheory";
    }

    @Override
    public String getSourceKey() {
        return "loltheory.gg";
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {
        if (patchString == null) {
            initPatchString();
        }
        try {
            String json =
                    Pipe.from(new URL(String.format(CHAMPION_URL, patchString, data.getChampion()
                            .getInternalName())).openStream())
                            .toString();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (String s : obj.keySet()) {
                Position pos = getPosition(s);
                if (pos != null) {
                    JsonObject item = obj.getAsJsonObject(s);
                    while (!item.has("secondary_documents")) {
                        item = item.getAsJsonObject(String.valueOf(item.getAsJsonObject("info")
                                .getAsJsonArray("selection")
                                .get(0)
                                .getAsInt()));
                    }
                    item = item.getAsJsonObject("secondary_documents").getAsJsonObject("runes_and_stat_shards");
                    RunePage p = new RunePage();
                    JsonArray mainRunes = item.getAsJsonObject("runes")
                            .getAsJsonArray("selection")
                            .get(0)
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonArray();
                    JsonArray secRunes = item.getAsJsonObject("runes")
                            .getAsJsonArray("selection")
                            .get(0)
                            .getAsJsonArray()
                            .get(1)
                            .getAsJsonArray();
                    JsonArray shards = item.getAsJsonObject("stat_shards")
                            .getAsJsonArray("selection")
                            .get(0)
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonArray();
                    for (JsonElement r : mainRunes) {
                        p.getRunes().add(Rune.getById(r.getAsInt()));
                    }
                    for (JsonElement r : secRunes) {
                        p.getRunes().add(Rune.getById(r.getAsInt()));
                    }
                    for (JsonElement r : shards) {
                        p.getModifiers().add(Modifier.getById(r.getAsInt()));
                    }
                    p.setName(getPositionName(pos));
                    p.setChampion(data.getChampion());
                    p.setSource(String.format(PUBLIC_URL, data.getChampion().getInternalName().toLowerCase()));
                    p.setSourceName(getSourceName());
                    p.fixStyle();
                    p.fixOrder();
                    if (!p.verify()) {
                        continue;
                    }
                    pages.add(p);
                }
            }
        } catch (Exception e) {
            System.out.println(data.getChampion());
            e.printStackTrace();
        }
    }

    private static Position getPosition(String id) {
        switch (id) {
            case "0":
                return Position.TOP;
            case "1":
                return Position.JUNGLE;
            case "2":
                return Position.MIDDLE;
            case "3":
                return Position.BOTTOM;
            case "4":
                return Position.UTILITY;
            default:
                return null;
        }
    }

    private static String getPositionName(Position pos) {
        switch (pos) {
            case TOP:
                return "Top";
            case JUNGLE:
                return "Jungle";
            case MIDDLE:
                return "Mid";
            case BOTTOM:
                return "Adc";
            case UTILITY:
                return "Support";
            default:
                return null;
        }
    }

    private void initPatchString() {
        List<Patch> latest = Patch.getLatest(5);
        for (Patch value : latest) {
            patchString = value.format("%d_%d");
            try {
                if (!Pipe.from(new URL(String.format(PATCH_URL, patchString)).openStream()).toString().equals("null")) {
                    break;
                }
            } catch (IOException ignored) {
            }
        }
    }

}
