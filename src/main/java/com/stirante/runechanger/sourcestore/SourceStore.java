package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.controllers.SettingsCategoryController;
import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.sourcestore.impl.*;
import com.stirante.runechanger.util.SimplePreferences;
import com.stirante.runechanger.util.SyncingListWrapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SourceStore {

    private static final List<Source> sources = new ArrayList<>();

    static {
        sources.add(new UGGSource());
        sources.add(new RuneforgeSource());
        sources.add(new ChampionGGSource());
        sources.add(new LolalyticsSource());
        sources.add(new MetasrcSource());
        sources.add(new LocalSource());
    }

    public static void init() {
        for (Source source : sources) {
            updateSourceSettings(source);
        }
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
    public static void getRunes(GameData data, SyncingListWrapper<RunePage> pages) {
        sources.stream()
                .filter(source -> source instanceof RuneSource &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (RuneSource) source)
                .forEach(runeSource ->
                        RuneChanger.EXECUTOR_SERVICE.submit(() -> runeSource.getRunesForGame(data, pages))
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
                RuneChanger.EXECUTOR_SERVICE.submit(() -> {
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
    public static void getRemoteRunes(Champion champion, SyncingListWrapper<RunePage> pages) {
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            sources.stream()
                    .filter(source -> source instanceof RuneSource && !(source instanceof LocalSource) &&
                            SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                    .map(source -> (RuneSource) source)
                    .forEach(runeSource -> RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                                for (GameMode mode : runeSource.getSupportedGameModes()) {
                                    runeSource.getRunesForGame(GameData.of(champion, mode), pages);
                                }
                            })
                    );
        });
    }

    /**
     * Get list of local rune pages
     *
     * @param pages list of pages, which will be filled with pages
     */
    public static void getLocalRunes(SyncingListWrapper<RunePage> pages) {
        pages.addAll(SimplePreferences.getRuneBookValues());
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
}
