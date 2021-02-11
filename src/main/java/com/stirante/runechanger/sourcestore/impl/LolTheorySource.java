package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.PipeExtension;
import com.stirante.runechanger.util.SyncingListWrapper;
import generated.Position;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LolTheorySource implements RuneSource {
    private static final String ROLE_URL = "https://loltheory-1b4da.firebaseio.com/%s/champions/%s/info/role.json";
    private static final String SELECTION_URL =
            "https://loltheory-1b4da.firebaseio.com/%s/champions/%s/%s%sinfo/selection.json";
    private static final String RUNES_URL =
            "https://loltheory-1b4da.firebaseio.com/%s/champions/%s/%s%ssecondary_documents/runes_and_stat_shards.json";
    private static final String PUBLIC_URL = "https://loltheory.gg/champion/%s";
    private static final String PATCH_URL = "https://loltheory-1b4da.firebaseio.com/patch.json";
    private String patchString;

    private final Map<Champion, List<RunePage>> cache = new HashMap<>();

    @Override
    public String getSourceName() {
        return "LoLTheory";
    }

    @Override
    public String getSourceKey() {
        return "loltheory.gg";
    }

    private List<Integer> getRoles(Champion champion) {
        try {
            String url = String.format(ROLE_URL, patchString, champion.getInternalName());
            JsonArray roles = Pipe.from(new URL(url)).to(PipeExtension.JSON_ARRAY);
            return StreamSupport.stream(roles.spliterator(), false)
                    .map(JsonElement::getAsInt)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private JsonObject getRunes(Champion champion, int role) {
        try {
            String items = "/";
            for (int i = 0; i < 3; i++) {
                String url = String.format(SELECTION_URL, patchString, champion.getInternalName(), role, items);
                JsonArray list = Pipe.from(new URL(url)).to(PipeExtension.JSON_ARRAY);
                items += list.get(0).getAsInt() + "/";
            }
            String url = String.format(RUNES_URL, patchString, champion.getInternalName(), role, items);
            return Pipe.from(new URL(url)).to(PipeExtension.JSON_OBJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {
        if (cache.containsKey(data.getChampion())) {
            pages.addAll(cache.get(data.getChampion()));
        }
        else {
            cache.put(data.getChampion(), new ArrayList<>());
        }
        if (patchString == null) {
            initPatchString();
        }
        try {
            List<Integer> roles = getRoles(data.getChampion());
            for (Integer role : roles) {
                JsonObject item = getRunes(data.getChampion(), role);
                if (item != null) {
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
                    p.setName(getPositionName(Objects.requireNonNull(getPosition(role))));
                    p.setChampion(data.getChampion());
                    p.setSource(String.format(PUBLIC_URL, data.getChampion().getInternalName().toLowerCase()));
                    p.setSourceName(getSourceName());
                    p.fixStyle();
                    p.fixOrder();
                    if (!p.verify()) {
                        continue;
                    }
                    pages.add(p);
                    cache.get(data.getChampion()).add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Position getPosition(int id) {
        switch (id) {
            case 0:
                return Position.TOP;
            case 1:
                return Position.JUNGLE;
            case 2:
                return Position.MIDDLE;
            case 3:
                return Position.BOTTOM;
            case 4:
                return Position.UTILITY;
            default:
                return Position.UNSELECTED;
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
        try {
            patchString = StreamSupport.stream(Pipe.from(new URL(PATCH_URL)).to(PipeExtension.JSON_ARRAY).spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .map(jsonObject -> jsonObject.get("key").getAsString())
                    .map(Patch::fromString)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("LoLTheory source does not have a proper patch version!"))
                    .format("%d_%d");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
