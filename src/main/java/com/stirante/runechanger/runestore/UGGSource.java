package com.stirante.runechanger.runestore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.StringUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(UGGSource.class);

    private static final String UGG_VERSION = "1.1";
    private static final String OVERVIEW_VERSION = "1.2.6";
    private static final String BASE_URL =
            "https://stats2.u.gg/lol/" + UGG_VERSION + "/overview/%patch%/ranked_solo_5x5/%championId%/" +
                    OVERVIEW_VERSION + ".json";
    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String PUBLIC_URL = "https://u.gg/lol/champions/%championName%/build";
    // Builds with game count lower than this won't be included in result
    private static final Tier DEFAULT_TIER = Tier.PLATINUM_PLUS;
    private static final Server DEFAULT_SERVER = Server.WORLD;

    private static String patchString = null;

    public static void main(String[] args) throws IOException {
        DebugConsts.enableDebugMode();
        Champion.init();
        ObservableList<RunePage> pages = FXCollections.observableArrayList();
        new UGGSource().getRunesForChampion(Champion.getByName("blitzcrank"), pages);
        for (RunePage page : pages) {
            System.out.println(page);
        }
    }

    public RunePage getForChampion(Champion champion, Tier tier, Position position, Server server) throws IOException {
        return getForChampion(getRootObject(champion), champion, tier, position, server);
    }

    private JsonObject getRootObject(Champion champion) throws IOException {
        if (patchString == null) {
            initPatchString();
        }
        URL url = new URL(BASE_URL.replace("%patch%", patchString)
                .replace("%championId%", Integer.toString(champion.getId())));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in = urlConnection.getInputStream();
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(new InputStreamReader(in)).getAsJsonObject();
        in.close();
        return root;
    }

    private RunePage getForChampion(JsonObject root, Champion champion, Tier tier, Position position, Server server) {
        if (!root.has(server.toString()) ||
                !root.getAsJsonObject(server.toString()).has(tier.toString()) ||
                !root.getAsJsonObject(server.toString()).getAsJsonObject(tier.toString()).has(position.toString())) {
            return null;
        }
        JsonArray arr = root
                .getAsJsonObject(server.toString())
                .getAsJsonObject(tier.toString())
                .getAsJsonArray(position.toString())
                .get(0).getAsJsonArray();
        int games = arr.get(OverviewElement.RUNE_PAGES.getKey()).getAsJsonArray().get(Page.GAMES.getKey()).getAsInt();
        log.debug("Games count for " + champion.getName() + " on " + position.name() + ": " + games);
        RunePage page = new RunePage();
        page.setSourceName(getSourceName());
        page.setMainStyle(Style.getById(arr.get(OverviewElement.RUNE_PAGES.getKey())
                .getAsJsonArray()
                .get(Page.MAIN_STYLE.getKey())
                .getAsInt()));
        page.setSubStyle(Style.getById(arr.get(OverviewElement.RUNE_PAGES.getKey())
                .getAsJsonArray()
                .get(Page.SUB_STYLE.getKey())
                .getAsInt()));
        for (JsonElement element : arr.get(OverviewElement.RUNE_PAGES.getKey())
                .getAsJsonArray()
                .get(Page.RUNES.getKey())
                .getAsJsonArray()) {
            page.getRunes().add(Rune.getById(element.getAsInt()));
        }

        for (JsonElement element : arr.get(OverviewElement.MODIFIERS.getKey())
                .getAsJsonArray()
                .get(2)
                .getAsJsonArray()) {
            page.getModifiers().add(Modifier.getById(element.getAsInt()));
        }
        page.setName(StringUtils.fromEnumName(position.name()));
        page.setChampion(champion);
        page.setSource(PUBLIC_URL.replace("%championName%", champion.getName().toLowerCase()));
        page.fixOrder();
        if (!page.verify()) {
            return null;
        }
        return page;
    }

    @Override
    public String getSourceName() {
        return "u.gg";
    }

    @Override
    public void getRunesForChampion(Champion champion, ObservableList<RunePage> pages) {
        try {
            JsonObject root = getRootObject(champion);
            for (Position position : Position.values()) {
                RunePage page = getForChampion(root, champion, DEFAULT_TIER, position, DEFAULT_SERVER);
                if (page == null) {
                    continue;
                }
                FxUtils.doOnFxThread(() -> pages.add(page));
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting UGG rune page for champion " + champion.getName(), e);
        }
    }

    private void initPatchString() {
        try {
            URL url = new URL(VERSIONS_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            JsonArray strings = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonArray();
            int i = 0;
            while (patchString == null) {
                String[] patch = strings.get(i).getAsString().split("\\.");
                patchString = patch[0] + "_" + patch[1];
                try {
                    getRootObject(Champion.getByName("annie"));
                } catch (IOException e) {
                    i++;
                    patchString = null;
                }
            }
            in.close();
        } catch (IOException e) {
            log.error("Exception occurred while initializing patch string", e);
        }
    }

    // All those enums are keys to a json file

    private enum Server {
        NA(1),
        EUW(2),
        KR(3),
        EUNE(4),
        BR(5),
        LAS(6),
        LAN(7),
        OCE(8),
        RU(9),
        TR(10),
        JP(11),
        WORLD(12);

        private final int key;

        Server(int key) {
            this.key = key;
        }

        public int getKey() {
            return key;
        }

        @Override
        public String toString() {
            return Integer.toString(key);
        }
    }

    private enum Tier {
        CHALLENGER(1),
        MASTER(2),
        DIAMOND(3),
        PLATINUM(4),
        GOLD(5),
        SILVER(6),
        BRONZE(7),
        OVERALL(8),
        PLATINUM_PLUS(10),
        DIAMOND_PLUS(11);

        private final int key;

        Tier(int key) {
            this.key = key;
        }

        public int getKey() {
            return key;
        }

        @Override
        public String toString() {
            return Integer.toString(key);
        }
    }

    private enum Position {
        JUNGLE(1),
        SUPPORT(2),
        ADC(3),
        TOP(4),
        MID(5);

        private final int key;

        Position(int key) {
            this.key = key;
        }

        public int getKey() {
            return key;
        }

        @Override
        public String toString() {
            return Integer.toString(key);
        }
    }

    private enum OverviewElement {
        RUNE_PAGES(0),
        SUMMONER_SPELLS(1),
        STARTING_BUILD(2),
        CORE_BUILD(3),
        SKILL_ORDER(4),
        ITEM_OPTIONS(4),
        DATA_SAMPLE(6),
        MODIFIERS(8);

        private final int key;

        OverviewElement(int key) {
            this.key = key;
        }

        public int getKey() {
            return key;
        }

        @Override
        public String toString() {
            return Integer.toString(key);
        }
    }

    private enum Page {
        GAMES(0),
        WON(1),
        MAIN_STYLE(2),
        SUB_STYLE(3),
        RUNES(4);

        private final int key;

        Page(int key) {
            this.key = key;
        }

        public int getKey() {
            return key;
        }

        @Override
        public String toString() {
            return Integer.toString(key);
        }
    }
}
