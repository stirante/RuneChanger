package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.justpipe.Pipe;
import com.stirante.lolclient.libs.org.apache.http.client.methods.HttpGet;
import com.stirante.lolclient.libs.org.apache.http.impl.client.CloseableHttpClient;
import com.stirante.lolclient.libs.org.apache.http.impl.client.HttpClients;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.utils.StringUtils;
import com.stirante.runechanger.utils.SyncingListWrapper;
import generated.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChampionGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);
    private static final ReentrantLock LOCK = new ReentrantLock();

    private final static String ALL_URL =
            "https://league-champion-aggregate.iesdev.com/api/champions?queue=%queue%&rank=%rank%&region=world";
    private final static String CHAMPION_API_URL =
            "https://league-champion-aggregate.iesdev.com/api/champions/%champion%?queue=%queue%&rank=%rank%&region=world";
    private final static String CHAMPION_URL = "https://champion.gg/champion/";
    private final static HashMap<Champion, Position> positionCache = new HashMap<>();
    private final static HashMap<Champion, Map<Position, ChampionBuild>> pageCache = new HashMap<>();
    private static boolean initialized = false;
    private static final Tier DEFAULT_TIER = Tier.PLATINUM_PLUS;
    private static final Queue DEFAULT_QUEUE = Queue.RANKED_SOLO_DUO;

    private int minThreshold = 0;
    private boolean mostCommon = false;

    private Position toPosition(String s) {
        switch (s) {
            case "SUPPORT":
                return Position.UTILITY;
            case "MID":
                return Position.MIDDLE;
            case "ADC":
                return Position.BOTTOM;
            case "TOP":
                return Position.TOP;
            case "JUNGLE":
                return Position.JUNGLE;
            default:
                log.warn("Unknown position name: " + s);
                return Position.UNSELECTED;
        }
    }

    private void initCache() {
        LOCK.lock();
        if (initialized) {
            LOCK.unlock();
            return;
        }
        try {
            String json;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(ALL_URL
                        .replaceAll("%queue%", String.valueOf(DEFAULT_QUEUE.getId()))
                        .replaceAll("%rank%", DEFAULT_TIER.name()));
                request.setHeader("Origin", "https://champion.gg");
                json = Pipe.from(client.execute(request).getEntity().getContent()).toString();
            }
            JsonArray data = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("data");
            for (JsonElement champion : data) {
                JsonObject obj = champion.getAsJsonObject();
                Champion champ = Champion.getById(obj.get("champion_id").getAsInt());
                Position role = toPosition(obj.get("role").getAsString());
                if (!positionCache.containsKey(champ)) {
                    positionCache.put(champ, role);
                }
                if (!pageCache.containsKey(champ)) {
                    pageCache.put(champ, new HashMap<>());
                }
                JsonObject stats = obj.getAsJsonObject("stats");
                pageCache.get(champ)
                        .put(role, toChampionBuild(champ, obj.get("role")
                                .getAsString(), stats));
            }
        } catch (Exception e) {
            log.error("Exception occurred while initializing cache for ChampionGG source", e);
        }
        initialized = true;
        LOCK.unlock();
    }

    private ChampionBuild toChampionBuild(Champion champion, String position, JsonObject stats) {
        JsonObject runes = stats.getAsJsonObject((mostCommon ? "most_common_" : "") + "runes");
        JsonObject shards = stats.getAsJsonObject((mostCommon ? "most_common_" : "") + "rune_stat_shards");
        RunePage page = new RunePage();

        int i = 0;
        for (JsonElement build : runes.getAsJsonArray("build")) {
            if (i == 0) {
                page.setMainStyle(Style.getById(build.getAsInt()));
            }
            else if (i == 5) {
                page.setSubStyle(Style.getById(build.getAsInt()));
            }
            else {
                page.getRunes().add(Rune.getById(build.getAsInt()));
            }
            i++;
        }

        for (JsonElement build : shards.getAsJsonArray("build")) {
            page.getModifiers().add(Modifier.getById(build.getAsInt()));
        }

        page.setName(StringUtils.fromEnumName(position));
        page.setChampion(champion);
        page.setSource(CHAMPION_URL + champion.getName() + "/" + position);
        page.setSourceName(getSourceName());
        page.fixOrder();
        if (!page.verify()) {
            return null;
        }

        List<SummonerSpell> spells =
                StreamSupport.stream(stats.getAsJsonObject("spells").getAsJsonArray("build").spliterator(), false)
                        .map(jsonElement -> SummonerSpell.getByKey(jsonElement.getAsInt()))
                        .collect(Collectors.toList());

        return ChampionBuild.builder(page).withSpells(spells).create();
    }

    @Override
    public String getSourceName() {
        return "Champion.gg";
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        Map<Position, ChampionBuild> cache = pageCache.get(data.getChampion());
        if (!cache.isEmpty()) {
            pages.addAll(cache.values());
        }
        if (cache.size() < 3) {
            try {
                String json =
                        Pipe.from(new URL(CHAMPION_API_URL
                                .replaceAll("%champion%", String.valueOf(data.getChampion().getId()))
                                .replaceAll("%queue%", String.valueOf(DEFAULT_QUEUE.getId()))
                                .replaceAll("%rank%", DEFAULT_TIER.name())
                        ).openStream()).toString();
                JsonArray arr = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("data");
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    Position role = toPosition(obj.get("role").getAsString());
                    if (cache.containsKey(role)) {
                        continue;
                    }
                    JsonObject stats = obj.getAsJsonObject("stats");
                    if (stats.get("games").getAsInt() > minThreshold) {
                        ChampionBuild page = toChampionBuild(data.getChampion(), obj.get("role")
                                .getAsString(), stats);
                        pages.add(page);
                        cache.put(role, page);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getSourceKey() {
        return "champion.gg";
    }

    public Position getPositionForChampion(Champion champion) {
        initCache();
        if (!positionCache.containsKey(champion)) {
            log.warn("Champion not found: " + champion.getName());
            return Position.UNSELECTED;
        }
        return positionCache.get(champion);
    }

    @Override
    public void onSettingsUpdate(Map<String, Object> settings) {
        if (settings.containsKey("min_threshold")) {
            minThreshold = Integer.parseInt((String) settings.get("min_threshold"));
        }
        if (settings.containsKey("most_common")) {
            mostCommon = (Boolean) settings.get("most_common");
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
                .add()
                .checkbox("most_common")
                .defaultValue(false)
                .add();
    }

    private enum Tier {
        CHALLENGER,
        GRANDMASTER,
        MASTER,
        DIAMOND,
        PLATINUM,
        GOLD,
        SILVER,
        BRONZE,
        IRON,
        PLATINUM_PLUS
    }

    private enum Queue {
        RANKED_SOLO_DUO(420),
        RANKED_FLEX(440),
        NORMAL_DRAFT(400),
        NORMAL_BLIND(430),
        ARAM(450);

        private final int id;

        public int getId() {
            return id;
        }

        Queue(int id) {
            this.id = id;
        }
    }

    public static void main(String[] args) throws IOException {
        Champion.init();
        ChampionGGSource source = new ChampionGGSource();
        source.initCache();
        SourceStore.testSource(source, GameMode.CLASSIC);
    }
}
