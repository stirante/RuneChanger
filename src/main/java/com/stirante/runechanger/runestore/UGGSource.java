package com.stirante.runechanger.runestore;

import com.google.gson.*;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.Rune;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.model.client.Style;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class UGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(UGGSource.class);

    private static final String UGG_VERSION = "1.1";
    private static final String OVERVIEW_VERSION = "1.2.6";
    private static final String BASE_URL =
            "https://stats2.u.gg/lol/" + UGG_VERSION + "/overview/%patch%/ranked_solo_5x5/%championId%/" +
                    OVERVIEW_VERSION + ".json";
    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final Gson GSON = new GsonBuilder().create();

    private static String patchString = null;

    public static void main(String[] args) throws IOException {
        Champion.init();
        new UGGSource().getForChampion(Champion.getById(6), FXCollections.observableArrayList());
    }

    @Override
    public String getSourceName() {
        return "u.gg";
    }

    @Override
    public void getForChampion(Champion champion, ObservableList<RunePage> pages) {
        if (patchString == null) {
            initPatchString();
        }
        try {
            URL url = new URL(BASE_URL.replace("%patch%", patchString)
                    .replace("%championId%", Integer.toString(champion.getId())));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            JsonParser parser = new JsonParser();
            JsonObject rootObj = parser.parse(new InputStreamReader(in)).getAsJsonObject();
            JsonArray perks = rootObj
                    .getAsJsonObject(Server.WORLD.toString())
                    .getAsJsonObject(Tier.PLATINUM_PLUS.toString())
                    .getAsJsonArray(Position.TOP.toString())
                    .get(0).getAsJsonArray()
                    .get(Stat.PERKS.getKey()).getAsJsonArray();
            System.out.println(Perk.MAIN_PERK.name() + ": " + Style.getById(perks.get(Perk.MAIN_PERK.getKey()).getAsInt()));
            System.out.println(Perk.SUB_PERK.name() + ": " + Style.getById(perks.get(Perk.SUB_PERK.getKey()).getAsInt()));
            for (JsonElement e : perks.get(Perk.PERKS.getKey()).getAsJsonArray()) {
                System.out.println(Rune.getById(e.getAsInt()));
            }
            System.out.println(rootObj
                    .getAsJsonObject(Server.WORLD.toString())
                    .getAsJsonObject(Tier.PLATINUM_PLUS.toString())
                    .getAsJsonArray(Position.ADC.toString()));
            // data[servers][tiers][positions][0][stats][perks/shards]
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initPatchString() {
        try {
            URL url = new URL(VERSIONS_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            String[] strings = GSON.fromJson(new InputStreamReader(in), String[].class);
            String[] patch = strings[0].split("\\.");
            patchString = patch[0] + "_" + patch[1];
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    private enum Stat {
        PERKS(0),
        SKILL_ORDER(4),
        STAT_SHARDS(8);

        private final int key;

        Stat(int key) {
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

    private enum Perk {
        GAMES(0),
        WON(1),
        MAIN_PERK(2),
        SUB_PERK(3),
        PERKS(4);

        private final int key;

        Perk(int key) {
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
