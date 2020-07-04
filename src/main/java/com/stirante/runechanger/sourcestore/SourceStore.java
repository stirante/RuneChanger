package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.sourcestore.impl.ChampionGGSource;
import com.stirante.runechanger.sourcestore.impl.LocalSource;
import com.stirante.runechanger.sourcestore.impl.RuneforgeSource;
import com.stirante.runechanger.sourcestore.impl.UGGSource;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SourceStore {

    private static final List<Source> sources = new ArrayList<>();

    static {
        sources.add(new UGGSource());
        sources.add(new RuneforgeSource());
        sources.add(new ChampionGGSource());
        sources.add(new LocalSource());
    }

    /**
     * Get list of rune pages for champion
     *
     * @param champion champion
     * @param pages    list of pages, which will be filled with pages
     */
    public static void getRunes(Champion champion, GameMode mode, ObservableList<RunePage> pages) {
        sources.stream()
                .filter(source -> source instanceof RuneSource &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (RuneSource) source)
                .forEach(runeSource ->
                        RuneChanger.EXECUTOR_SERVICE.submit(() -> runeSource.getRunesForChampion(champion, mode, pages))
                );
    }

    /**
     * Get list of rune pages for champion
     *
     * @param champion champion
     */
    public static Future<CounterData> getCounterData(Champion champion) {
        CompletableFuture<CounterData> result = new CompletableFuture<>();
        CounterSource counterSource = sources.stream()
                .filter(source -> source instanceof CounterSource &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (CounterSource) source)
                .findFirst().orElse(null);
        if (counterSource != null) {
            RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                result.complete(counterSource.getCounterData(champion));
            });
        }
        return result;
    }

    /**
     * Get list of rune pages for champion except local runes
     *
     * @param champion champion
     * @param pages    list of pages, which will be filled with pages
     */
    public static void getRemoteRunes(Champion champion, ObservableList<RunePage> pages) {
        sources.stream()
                .filter(source -> source instanceof RuneSource && !(source instanceof LocalSource) &&
                        SimplePreferences.getBooleanValue(source.getSourceKey(), true))
                .map(source -> (RuneSource) source)
                .forEach(runeSource -> {
                            if (runeSource.hasGameModeSpecific()) {
                                RuneChanger.EXECUTOR_SERVICE.submit(() -> runeSource.getRunesForChampion(champion, GameMode.ARAM, pages));
                            }
                            RuneChanger.EXECUTOR_SERVICE.submit(() -> runeSource.getRunesForChampion(champion, GameMode.CLASSIC, pages));
                        }
                );
    }

    /**
     * Get list of local rune pages
     *
     * @param pages list of pages, which will be filled with pages
     */
    public static void getLocalRunes(ObservableList<RunePage> pages) {
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

}
