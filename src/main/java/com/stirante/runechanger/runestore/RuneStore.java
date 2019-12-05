package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class RuneStore {

    private static final List<RuneSource> sources = new ArrayList<>();

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
    public static void getRunes(Champion champion, ObservableList<RunePage> pages) {
        for (RuneSource source : sources) {
            new Thread(() -> source.getForChampion(champion, pages)).start();
        }
    }

    /**
     * Get list of rune pages for champion except local runes
     *
     * @param champion champion
     * @param pages    list of pages, which will be filled with pages
     */
    public static void getRemoteRunes(Champion champion, ObservableList<RunePage> pages) {
        for (RuneSource source : sources) {
            if (source instanceof LocalSource) {
                continue;
            }
            new Thread(() -> source.getForChampion(champion, pages)).start();
        }
    }

    /**
     * Get list of local rune pages
     *
     * @param pages    list of pages, which will be filled with pages
     */
    public static void getLocalRunes(ObservableList<RunePage> pages) {
        pages.addAll(SimplePreferences.getRuneBookValues());
    }

    @SuppressWarnings("unchecked")
    public static <T extends RuneSource> T getSource(Class<T> clz) {
        for (RuneSource source : sources) {
            if (clz.isAssignableFrom(source.getClass())) {
                return (T) source;
            }
        }
        return null;
    }

}
