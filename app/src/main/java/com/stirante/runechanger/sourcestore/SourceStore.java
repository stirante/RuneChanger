package com.stirante.runechanger.sourcestore;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.client.ChampionsImpl;
import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.sourcestore.impl.*;
import com.stirante.runechanger.utils.AsyncTask;
import com.stirante.runechanger.utils.FxUtils;
import com.stirante.runechanger.utils.SimplePreferences;
import com.stirante.runechanger.utils.SyncingListWrapper;
import javafx.collections.ListChangeListener;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SourceStore {

    private static final List<Source> sources = new ArrayList<>();

    private static final Cache<GameData, List<ChampionBuild>> GAME_CACHE =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(4)).build();
    private static final Cache<Champion, List<ChampionBuild>> GUI_CACHE =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15)).maximumSize(5).build();

    static {
        sources.add(new UGGSource());
        sources.add(new ChampionGGSource());
        sources.add(new LolalyticsSource());
        sources.add(new MetasrcSource());
        sources.add(new LolTheorySource());
        sources.add(new AramAcademySource());
        sources.add(new LocalSource());
    }

    public static void init(RuneChangerApi api) {
        for (Source source : sources) {
            updateSourceSettings(source);
            source.init(api);
        }
    }

    public static void invalidateCaches() {
        GUI_CACHE.invalidateAll();
        GAME_CACHE.invalidateAll();
    }

    public static void updateSourceSettings(Source source) {
        SettingsConfiguration config = new SettingsConfiguration();
        source.setupSettings(config);
        Map<String, Object> data = new HashMap<>();
        for (SettingsConfiguration.FieldConfiguration<?> field : config.getFields()) {
            Object val = field.getDefaultValue();
            if (field.getType() == Boolean.class) {
                val = SimplePreferences.getBooleanValue(field.getPrefKey(source.getSourceKey()), (boolean) field.getDefaultValue());
            }
            else if (field.getType() == String.class) {
                val = SimplePreferences.getStringValue(field.getPrefKey(source.getSourceKey()), (String) field.getDefaultValue());
            }
            data.put(field.getKey(), val);
        }
        source.onSettingsUpdate(data);
    }

    /**
     * Get list of rune pages for champion
     *
     * @param data  game data
     * @param pages list of pages, which will be filled with pages
     */
    public static void getRunes(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        List<ChampionBuild> cached = GAME_CACHE.getIfPresent(data);
        if (cached != null) {
            pages.addAll(cached);
            return;
        }
        else {
            GAME_CACHE.put(data, new ArrayList<>());
            pages.getBackingList().addListener((ListChangeListener.Change<? extends ChampionBuild> c) -> {
                List<ChampionBuild> list = GAME_CACHE.getIfPresent(data);
                if (list != null && !c.getList().isEmpty()) {
                    list.clear();
                    list.addAll(c.getList());
                }
            });
        }
        sources.stream()
                .filter(source -> source instanceof RuneSource &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (RuneSource) source)
                .forEach(runeSource ->
                        AsyncTask.EXECUTOR_SERVICE.submit(() -> runeSource.getRunesForGame(data, pages))
                );
    }

    /**
     * Get list of rune pages for champion
     *
     * @param champion champion
     */
    public static Future<CounterData> getCounterData(Champion champion) {
        CompletableFuture<CounterData> result = new CompletableFuture<>();
        sources.stream()
                .filter(source -> source instanceof CounterSource &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (CounterSource) source)
                .findFirst().ifPresent(counterSource ->
                        AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                            result.complete(counterSource.getCounterData(champion));
                        }));
        return result;
    }

    /**
     * Get list of rune pages for champion except local runes
     *
     * @param champion champion
     * @param pages    list of pages, which will be filled with pages
     */
    public static void getRemoteRunes(Champion champion, SyncingListWrapper<ChampionBuild> pages) {
        List<ChampionBuild> cached = GUI_CACHE.getIfPresent(champion);
        if (cached != null) {
            pages.addAll(cached);
            return;
        }
        else {
            GUI_CACHE.put(champion, new ArrayList<>());
            pages.getBackingList().addListener((ListChangeListener.Change<? extends ChampionBuild> c) -> {
                List<ChampionBuild> list = GUI_CACHE.getIfPresent(champion);
                if (list != null && !c.getList().isEmpty()) {
                    list.clear();
                    list.addAll(c.getList());
                }
            });
        }
        AsyncTask.EXECUTOR_SERVICE.submit(() -> {
            sources.stream()
                    .filter(source -> source instanceof RuneSource && !(source instanceof LocalSource) &&
                            SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                    .map(source -> (RuneSource) source)
                    .forEach(runeSource -> AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                                for (GameMode mode : runeSource.getSupportedGameModes()) {
                                    runeSource.getRunesForGame(GameData.of(champion, mode), pages);
                                }
                            })
                    );
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends Source> T getSource(Class<T> clz) {
        for (Source source : sources) {
            if (clz.isAssignableFrom(source.getClass())) {
                return (T) source;
            }
        }
        return null;
    }

    public static List<Source> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /**
     * Gets build for blitzcrank from provided source. Purely for testing whether source works locally. Don't use!
     */
    public static void testSource(RuneSource source, GameMode gameMode) throws IOException {
        FxUtils.DEBUG_WITHOUT_TOOLKIT = true;
        DebugConsts.enableDebugMode();
        ChampionsImpl champions = new ChampionsImpl(null);
        champions.init();
        SyncingListWrapper<ChampionBuild> pages = new SyncingListWrapper<>();
        source.getRunesForGame(GameData.of(champions.getByName("blitzcrank"), gameMode), pages);
        for (ChampionBuild page : pages.getBackingList()) {
            if (page.hasSummonerSpells()) {
                System.out.println(page.getFirstSpell().getName() + ", " + page.getSecondSpell().getName());
            }
            System.out.println(page.getRunePage());
        }
        System.exit(0);
    }

    /**
     * Gets build for all champions from provided source. Purely for testing whether source works locally. Don't use!
     */
    public static void testSourceAllChamps(RuneSource source, GameMode gameMode) throws IOException {
        FxUtils.DEBUG_WITHOUT_TOOLKIT = true;
        DebugConsts.enableDebugMode();
        ChampionsImpl champions = new ChampionsImpl(null);
        champions.init();
        SyncingListWrapper<ChampionBuild> pages = new SyncingListWrapper<>();
        for (Champion champion : champions.getChampions()) {
            source.getRunesForGame(GameData.of(champion, gameMode), pages);
            for (ChampionBuild page : pages.getBackingList()) {
                System.out.println(page.getRunePage());
            }
            pages.clear();
        }
        System.exit(0);
    }
}
