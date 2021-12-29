package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.utils.SyncingListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AramAcademySource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(AramAcademySource.class);

    private static final String BASE_URL = "https://aram.academy/api/builds/%champion%";
    private static final String PUBLIC_URL = "https://aram.academy/champions/%champion%";

    public static void main(String[] args) throws IOException {
        AramAcademySource source = new AramAcademySource();
        SourceStore.testSource(source, GameMode.ARAM);
    }

    @Override
    public String getSourceKey() {
        return "aram.academy";
    }

    @Override
    public String getSourceName() {
        return "aram.academy";
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        if (data.getGameMode() != GameMode.ARAM) {
            return;
        }
        try {
            URL url = new URL(BASE_URL.replace("%champion%", data.getChampion().getInternalName()));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
            in.close();

            JsonObject runes = root.getAsJsonArray("runes").get(0).getAsJsonObject();

            RunePage page = new RunePage();
            page.setSourceName(getSourceName());
            page.setMainStyle(Style.getByName(runes.get("runes_primary").getAsString()));
            page.setSubStyle(Style.getByName(runes.get("runes_secondary").getAsString()));
            for (JsonElement element : runes.getAsJsonArray("runes_primary_list")) {
                page.getRunes().add(Rune.getByName(element.getAsString()));
            }
            for (JsonElement element : runes.getAsJsonArray("runes_secondary_list")) {
                page.getRunes().add(Rune.getByName(element.getAsString()));
            }

            for (JsonElement element : runes.getAsJsonArray("runes_stats")) {
                page.getModifiers().add(getModifierByName(element.getAsString()));
            }
            page.setName("ARAM");
            page.setChampion(data.getChampion());
            page.setSource(PUBLIC_URL.replace("%champion%", data.getChampion().getInternalName()));
            page.fixOrder();
            if (!page.verify()) {
                return;
            }
            JsonArray summonerSpells = root.getAsJsonArray("summoner_spells").get(0).getAsJsonObject().getAsJsonArray("spells");
            List<SummonerSpell> spells = StreamSupport.stream(summonerSpells.spliterator(), false)
                    .map(jsonElement -> SummonerSpell.getByName(jsonElement.getAsString()))
                    .collect(Collectors.toList());
            pages.add(ChampionBuild.builder(page).withSpells(spells).create());
        } catch (IOException e) {
            log.error("Exception occurred while getting aram.academy rune page for champion " +
                    data.getChampion().getName(), e);
        }
    }

    private static Modifier getModifierByName(String name) {
        switch (name) {
            case "CDRScaling":
                return Modifier.RUNE_5007;
            case "Armor":
                return Modifier.RUNE_5002;
            case "Adaptive":
                return Modifier.RUNE_5008;
            case "AttackSpeed":
                return Modifier.RUNE_5005;
            case "MagicRes":
                return Modifier.RUNE_5003;
            case "HealthScaling":
                return Modifier.RUNE_5001;
        }
        throw new IllegalArgumentException("Unknown modifier " + name);
    }

    @Override
    public GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.ARAM};
    }

}
