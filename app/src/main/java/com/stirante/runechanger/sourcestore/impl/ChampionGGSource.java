package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.api.*;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.utils.StringUtils;
import com.stirante.runechanger.utils.SyncingListWrapper;
import generated.Position;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChampionGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final String[] ROLES = {"TOP", "JUNGLE", "MID", "ADC", "SUPPORT"};
    private static final CloseableHttpClient client = HttpClients.createDefault();

    private final static String GRAPHQL_URL =
            "https://league-champion-aggregate.iesdev.com/graphql";
    private final static String ROLES_QUERY_DATA = "{\n" +
            "\tallChampionStats(\n" +
            "\t\ttier: %rank%\n" +
            "\t\tmostPopular: true\n" +
            "\t\tregion: WORLD\n" +
            "\t\tqueue: %queue%\n" +
            "\t) {\n" +
            "\t\tpatch\n" +
            "\t\trole\n" +
            "\t\trolePercentage\n" +
            "\t\tchampionId\n" +
            "\t\twins\n" +
            "\t\tlaneWins\n" +
            "\t\tgames\n" +
            "\t\ttotalGameCount\n" +
            "\t}\n" +
            "}\n";
    private final static String BUILD_QUERY_DATA = "{\n" +
            "\tchampionBuildStats(\n" +
            "\t\tchampionId: %champion%\n" +
            "\t\tqueue: %queue%\n" +
            "\t\trole: %role%\n" +
            "\t\topponentChampionId: null\n" +
            "\t\tkey: PUBLIC\n" +
            "\t) {\n" +
            "\t\tbuilds {\n" +
            "\t\t\tgames\n" +
            "\t\t\tmythicId\n" +
            "\t\t\tmythicAverageIndex\n" +
            "\t\t\tprimaryRune\n" +
            "\t\t\trunes {\n" +
            "\t\t\t\tgames\n" +
            "\t\t\t\tindex\n" +
            "\t\t\t\truneId\n" +
            "\t\t\t\twins\n" +
            "\t\t\t\ttreeId\n" +
            "\t\t\t}\n" +
            "\t\t\tsummonerSpells {\n" +
            "\t\t\t\tgames\n" +
            "\t\t\t\tsummonerSpellIds\n" +
            "\t\t\t\twins\n" +
            "\t\t\t}\n" +
            "\t\t\twins\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}\n";
    private final static String CHAMPION_URL = "https://champion.gg/champion/";
    private final static HashMap<Champion, Position> positionCache = new HashMap<>();
    private final static HashMap<Champion, Map<Position, ChampionBuild>> pageCache = new HashMap<>();
    private static boolean initialized = false;
    private static final Tier DEFAULT_TIER = Tier.PLATINUM_PLUS;
    private static final Queue DEFAULT_QUEUE = Queue.RANKED_SOLO_5X5;

    private int minThreshold = 0;

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

    private void initCache(Champions champions) {
        LOCK.lock();
        if (initialized) {
            LOCK.unlock();
            return;
        }
        try {
            String json;
            //TODO: This code returns a maximum of 200 entries and least played champions do not make it into the list
                HttpPost request = new HttpPost(GRAPHQL_URL);
                request.setHeader("Origin", "https://champion.gg");
                request.setEntity(new StringEntity(ROLES_QUERY_DATA
                        .replaceAll("%queue%", DEFAULT_QUEUE.name())
                        .replaceAll("%rank%", DEFAULT_TIER.name())));
                json = Pipe.from(client.execute(request).getEntity().getContent()).toString();
            JsonArray data = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("allChampionStats");
            StreamSupport.stream(data.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .collect(Collectors.groupingBy(jsonElement -> jsonElement.get("championId").getAsInt()))
                    .values().stream()
                    .map(roles -> roles.stream()
                            .max(Comparator.comparingInt(jsonElement -> jsonElement.getAsJsonObject().get("games").getAsInt())).orElseThrow())
                    .forEach(obj -> {
                        Champion champ = champions.getById(obj.get("championId").getAsInt());
                        Position role = toPosition(obj.get("role").getAsString());
                        if (!positionCache.containsKey(champ)) {
                            positionCache.put(champ, role);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception occurred while initializing cache for ChampionGG source", e);
        }
        initialized = true;
        LOCK.unlock();
    }

    private ChampionBuild toChampionBuild(Champion champion, String position, JsonObject stats) {
        RunePage page = new RunePage();

        List<JsonObject> runes = StreamSupport.stream(stats.get("runes").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.groupingBy(jsonElement -> jsonElement.get("index").getAsInt()))
                .values().stream()
                .map(jsonElements -> jsonElements.stream()
                        .max(Comparator.comparingDouble(jsonElement -> jsonElement.getAsJsonObject()
                                .get("wins")
                                .getAsDouble() / jsonElement.getAsJsonObject()
                                .get("games")
                                .getAsDouble()))
                        .orElseThrow())
                .sorted(Comparator.comparingInt(o -> o.get("index").getAsInt()))
                .collect(Collectors.toList());

        page.getRunes().add(Rune.getById(stats.get("primaryRune").getAsInt()));

        for (JsonObject build : runes) {
            int i = build.get("index").getAsInt();
            if (i == 0) {
                page.setMainStyle(Style.getById(build.get("treeId").getAsInt()));
            }
            else if (i == 3) {
                page.setSubStyle(Style.getById(build.get("treeId").getAsInt()));
            }
            if (i < 5) {
                page.getRunes().add(Rune.getById(build.get("runeId").getAsInt()));
            } else {
                page.getModifiers().add(Modifier.getById(build.get("runeId").getAsInt()));
            }
        }

        page.setName(StringUtils.fromEnumName(position));
        page.setChampion(champion);
        page.setSource(CHAMPION_URL + champion.getId());
        page.setSourceName(getSourceName());
        page.fixOrder();
        if (!page.verify()) {
            log.error("Invalid rune page for champion {} at {}", champion.getName(), position);
            System.out.println(StreamSupport.stream(stats.get("runes").getAsJsonArray().spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .map(e -> {
                        int index = e.get("index").getAsInt();
                        int runeId = e.get("runeId").getAsInt();
                        String rune = index >
                                4 ? String.valueOf(Modifier.getById(runeId)) : String.valueOf(Rune.getById(runeId));
                        return "games: " + e.get("games") + ", wins: " + e.get("wins") + ", rune: " + rune +
                                ", treeId: " + e.get("treeId") + ", index: " + index;
                    })
                    .collect(Collectors.joining("\n")));
            System.out.println();
            System.out.println(page.getRunes());
            System.out.println(page.getModifiers());
            return null;
        }

        List<SummonerSpell> spells =
                StreamSupport.stream(stats.getAsJsonArray("summonerSpells").get(0).getAsJsonObject().getAsJsonArray("summonerSpellIds").spliterator(), false)
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
        if (cache != null) {
            pages.addAll(cache.values());
        } else {
            cache = new HashMap<>();
            for (String role : ROLES) {
                try {
                    String json;
                    try (CloseableHttpClient client = HttpClients.createDefault()) {
                        HttpPost request = new HttpPost(GRAPHQL_URL);
                        request.setHeader("Origin", "https://champion.gg");
                        request.setEntity(new StringEntity(BUILD_QUERY_DATA
                                .replaceAll("%champion%", String.valueOf(data.getChampion().getId()))
                                .replaceAll("%queue%", DEFAULT_QUEUE.name())
                                .replaceAll("%role%", role)
                                .replaceAll("%rank%", DEFAULT_TIER.name())));
                        json = Pipe.from(client.execute(request).getEntity().getContent()).toString();
                    }
                    JsonObject obj = StreamSupport.stream(JsonParser.parseString(json)
                            .getAsJsonObject().getAsJsonObject("data")
                            .getAsJsonObject("championBuildStats")
                            .getAsJsonArray("builds").spliterator(), false)
                            .map(JsonElement::getAsJsonObject).max(Comparator.comparingInt(o -> o.get("games").getAsInt()))
                            .orElseThrow();
                        if (obj.get("games").getAsInt() > minThreshold) {
                            ChampionBuild page = toChampionBuild(data.getChampion(), role, obj);
                            if (page != null) {
                                pages.add(page);
                                cache.put(toPosition(role), page);
                            }
                        }
                } catch (Exception e) {
                    log.error("Failed to get build for {} {}", data.getChampion(), role, e);
                }
            }
            pageCache.put(data.getChampion(), cache);
        }
    }

    @Override
    public String getSourceKey() {
        return "champion.gg";
    }

    public Position getPositionForChampion(Champion champion) {
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
    public void init(RuneChangerApi api) {
        initCache(api.getChampions());
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
        CLASH_SR,
        HOWLING_ABYSS_ARAM,
        HOWLING_ABYSS_PORO_KING,
        RANKED_FLEX_SR,
        RANKED_SOLO_5X5,
        SUMMONERS_RIFT_BLIND_PICK,
        SUMMONERS_RIFT_DRAFT_PICK,
        SUMMONERS_RIFT_URF
    }

}
