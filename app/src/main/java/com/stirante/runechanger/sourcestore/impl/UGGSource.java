package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.runechanger.api.*;
import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.Patch;
import com.stirante.runechanger.sourcestore.CounterSource;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.utils.StringUtils;
import com.stirante.runechanger.utils.SyncingListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UGGSource implements RuneSource, CounterSource {
    private static final Logger log = LoggerFactory.getLogger(UGGSource.class);

    private static final String UGG_VERSION = "1.5";
    private static final String OVERVIEW_VERSION = "1.5.0";
    private static final String COUNTERS_VERSION = "1.5.0";
    private static final String OVERVIEW_PATH = "overview";
    private static final String COUNTERS_PATH = "counters";
    private static final String BASE_URL =
            "https://stats2.u.gg/lol/" + UGG_VERSION + "/%path%/%patch%/%queue%/%championId%/%version%.json";
    private static final String PUBLIC_URL = "https://u.gg/lol/champions/%championName%/build";
    private static final Tier DEFAULT_TIER = Tier.PLATINUM_PLUS;
    private static final Server DEFAULT_SERVER = Server.WORLD;

    private static String patchString = null;
    private int minThreshold = 0;
    private RuneChangerApi api;

    public static void main(String[] args) throws IOException {
        UGGSource source = new UGGSource();
        SourceStore.testSource(source);
    }

    @Override
    public void init(RuneChangerApi api) {
        this.api = api;
    }

    @Override
    public String getSourceKey() {
        return "u.gg";
    }

    private JsonObject getRootObject(Champion champion, String path, String version, QueueType queue) throws IOException {
        if (patchString == null) {
            initPatchString();
        }
        URL url = new URL(BASE_URL
                .replace("%path%", path)
                .replace("%patch%", patchString)
                .replace("%queue%", queue.getInternalName())
                .replace("%championId%", Integer.toString(champion.getId()))
                .replace("%version%", version));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in = urlConnection.getInputStream();
        JsonObject root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
        in.close();
        return root;
    }

    private ChampionBuild.Builder getForChampion(JsonObject root, Champion champion, Tier tier, Position position, Server server) {
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
        if (games < minThreshold) {
            return null;
        }
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
        List<SummonerSpell> spells = StreamSupport.stream(arr.get(OverviewElement.SUMMONER_SPELLS.getKey())
                        .getAsJsonArray()
                        .get(SummonerSpells.SPELLS.getKey())
                        .getAsJsonArray()
                        .spliterator(), false)
                .map(jsonElement -> SummonerSpell.getByKey(jsonElement.getAsInt()))
                .collect(Collectors.toList());
        return ChampionBuild.builder(page).withSpells(spells);
    }

    @Override
    public String getSourceName() {
        return "u.gg";
    }

    private static QueueType getQueueType(GameMode gameMode) {
        switch (gameMode) {
            case CLASSIC:
                return QueueType.RANKED_SOLO;
            case ARAM:
                return QueueType.ARAM;
            case URF:
                return QueueType.URF;
            default:
                return QueueType.RANKED_SOLO;
        }
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        try {
            JsonObject root = getRootObject(data.getChampion(), OVERVIEW_PATH, OVERVIEW_VERSION,
                    getQueueType(data.getGameMode()));
            if (data.getGameMode() == GameMode.ARAM) {
                ChampionBuild.Builder builder =
                        getForChampion(root, data.getChampion(), Tier.OVERALL, Position.NONE, DEFAULT_SERVER);
                if (builder == null) {
                    return;
                }
                pages.add(builder.withName("ARAM").create());
            }
            else if (data.getGameMode() == GameMode.URF) {
                ChampionBuild.Builder builder =
                        getForChampion(root, data.getChampion(), Tier.OVERALL, Position.NONE, DEFAULT_SERVER);
                if (builder == null) {
                    return;
                }
                pages.add(builder.withName("URF").create());
            }
            else {
                for (Position position : Position.values()) {
                    ChampionBuild.Builder builder =
                            getForChampion(root, data.getChampion(), DEFAULT_TIER, position, DEFAULT_SERVER);
                    if (builder == null) {
                        return;
                    }
                    pages.add(builder.create());
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting UGG rune page for champion " + data.getChampion().getName(), e);
        }
    }

    private void initPatchString() {
        List<Patch> latest = Patch.getLatest(5);
        for (Patch value : latest) {
            patchString = value.format("%d_%d");
            try {
                getRootObject(api.getChampions()
                        .getByName("annie"), OVERVIEW_PATH, OVERVIEW_VERSION, QueueType.RANKED_SOLO);
                break;
            } catch (IOException ignored) {
            }
        }
    }

    public CounterData getCounterData(Champion champion, Tier tier, Position position, Server server) throws IOException {
        if (patchString == null) {
            initPatchString();
        }
        JsonObject root = getRootObject(champion, COUNTERS_PATH, COUNTERS_VERSION, QueueType.RANKED_SOLO);
        if (!root.has(server.toString()) ||
                !root.getAsJsonObject(server.toString()).has(tier.toString()) ||
                (position != null && !root.getAsJsonObject(server.toString())
                        .getAsJsonObject(tier.toString())
                        .has(position.toString()))) {
            return null;
        }
        String pos = position == null ? root
                .getAsJsonObject(server.toString())
                .getAsJsonObject(tier.toString()).keySet().stream().findFirst().orElse(null) : position.toString();
        if (pos == null) {
            return new CounterData();
        }
        JsonArray arr = root
                .getAsJsonObject(server.toString())
                .getAsJsonObject(tier.toString())
                .getAsJsonArray(pos)
                .get(1).getAsJsonArray();
        Set<CounterChampion> strongAgainst =
                counterArrayToObjects(arr.get(CounterElement.STRONG_AGAINST_SAME_POSITION.getKey()).getAsJsonArray());
        strongAgainst.addAll(
                counterArrayToObjects(arr.get(CounterElement.STRONG_AGAINST_DIFFERENT_POSITION.getKey())
                        .getAsJsonArray()));
        Set<CounterChampion> weakAgainst =
                counterArrayToObjects(arr.get(CounterElement.WEAK_AGAINST_SAME_POSITION.getKey()).getAsJsonArray());
        weakAgainst.addAll(
                counterArrayToObjects(arr.get(CounterElement.WEAK_AGAINST_DIFFERENT_POSITION.getKey())
                        .getAsJsonArray()));

        Set<CounterChampion> weakWith =
                counterArrayToObjects(arr.get(CounterElement.WEAK_WITH.getKey()).getAsJsonArray());

        Set<CounterChampion> strongWith =
                counterArrayToObjects(arr.get(CounterElement.STRONG_WITH.getKey()).getAsJsonArray());

        CounterData result = new CounterData();
        result.setStrongAgainst(strongAgainst.stream()
                .sorted()
                .limit(5)
                .map(CounterChampion::getChampion)
                .collect(Collectors.toList()));
        result.setWeakAgainst(weakAgainst.stream()
                .sorted(Comparator.reverseOrder())
                .limit(5)
                .map(CounterChampion::getChampion)
                .collect(Collectors.toList()));
        result.setWeakWith(weakWith.stream()
                .sorted(Comparator.reverseOrder())
                .limit(5)
                .map(CounterChampion::getChampion)
                .collect(Collectors.toList()));
        result.setStrongWith(strongWith.stream()
                .sorted()
                .limit(5)
                .map(CounterChampion::getChampion)
                .collect(Collectors.toList()));

        return result;
    }

    private Set<CounterChampion> counterArrayToObjects(JsonArray arr) {
        Set<CounterChampion> result = new HashSet<>();
        for (JsonElement element : arr) {
            JsonArray jsonArray = element.getAsJsonArray();
            result.add(new CounterChampion(
                    api.getChampions().getById(jsonArray.get(Counter.CHAMPION.getKey()).getAsInt()),
                    Position.getByKey(jsonArray.get(Counter.POSITION.getKey()).getAsInt()),
                    jsonArray.get(Counter.WON.getKey()).getAsInt(),
                    jsonArray.get(Counter.GAMES.getKey()).getAsInt()
            ));
        }
        return result;
    }

    @Override
    public CounterData getCounterData(Champion champion) {
        try {
            return getCounterData(champion, DEFAULT_TIER, null, DEFAULT_SERVER);
        } catch (IOException e) {
            e.printStackTrace();
            return new CounterData();
        }
    }

    @Override
    public void onSettingsUpdate(Map<String, Object> settings) {
        if (settings.containsKey("min_threshold")) {
            minThreshold = Integer.parseInt((String) settings.get("min_threshold"));
        }
    }

    @Override
    public void setupSettings(SettingsConfiguration config) {
        config
                .textField("min_threshold")
                .defaultValue("0")
                .validation(s -> {
                    try {
                        return Integer.parseInt(s) >= 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .add();
    }

    @Override
    public GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.CLASSIC, GameMode.ARAM, GameMode.URF};
    }

    private static class CounterChampion implements Comparable<CounterChampion> {
        private final Champion champion;
        private final Position position;
        private final int wins;
        private final int games;

        public CounterChampion(Champion champion, Position position, int wins, int games) {
            this.champion = champion;
            this.position = position;
            this.wins = wins;
            this.games = games;
        }

        public Champion getChampion() {
            return champion;
        }

        public Position getPosition() {
            return position;
        }

        public int getWins() {
            return wins;
        }

        public int getGames() {
            return games;
        }

        public double getWinRate() {
            return (double) wins / (double) games;
        }

        @Override
        public int compareTo(UGGSource.CounterChampion counterChampion) {
            return Double.compare(counterChampion.getWinRate(), getWinRate());
        }
    }

    private enum QueueType {
        RANKED_SOLO("ranked_solo_5x5"),
        ARAM("normal_aram"),
        RANKED_FLEX("ranked_flex_sr"),
        NORMAL_BLIND("normal_blind_5x5"),
        NORMAL_DRAFT("normal_draft_5x5"),
        URF("urf");

        private final String internalName;

        QueueType(String internalName) {
            this.internalName = internalName;
        }

        public String getInternalName() {
            return internalName;
        }

        @Override
        public String toString() {
            return internalName;
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
        MID(5),
        NONE(6);

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

        public static Position getByKey(int key) {
            for (Position value : values()) {
                if (value.key == key) {
                    return value;
                }
            }
            return null;
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

    private enum CounterElement {
        STRONG_AGAINST_SAME_POSITION(0),
        WEAK_AGAINST_SAME_POSITION(1),
        STRONG_AGAINST_DIFFERENT_POSITION(2),
        WEAK_AGAINST_DIFFERENT_POSITION(3),
        STRONG_WITH(4),
        WEAK_WITH(5);

        private final int key;

        CounterElement(int key) {
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

    private enum Counter {
        CHAMPION(0),
        POSITION(1),
        WON(2),
        GAMES(3);

        private final int key;

        Counter(int key) {
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

    private enum SummonerSpells {
        GAMES(0),
        WON(1),
        SPELLS(2);

        private final int key;

        SummonerSpells(int key) {
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
