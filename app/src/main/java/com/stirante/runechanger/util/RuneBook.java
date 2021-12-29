package com.stirante.runechanger.util;

import com.stirante.eventbus.EventBus;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.client.Runes;
import com.stirante.runechanger.model.client.ChampionBuild;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class RuneBook {
    private static final Logger log = LoggerFactory.getLogger(RuneBook.class);

    private static final String RUNEBOOK_FILENAME =
            new File(PathUtils.getWorkingDirectory(), "RuneChangerRuneBook.dat").getAbsolutePath();
    private static ArrayList<ChampionBuild> runeBookValues;

    public static ArrayList<ChampionBuild> getRuneBookValues() {
        return runeBookValues;
    }

    public static void loadRuneBook() {
        File runeBookValuesFile = new File(RUNEBOOK_FILENAME);
        try (DataInputStream ois = new DataInputStream(new FileInputStream(runeBookValuesFile))) {
            runeBookValues = new ArrayList<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                RunePage p = new RunePage();
                p.deserialize(ois);
                runeBookValues.add(ChampionBuild.builder(p).create());
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                log.error("Exception occurred while loading rune book file", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while loading rune book file", false);
            }
        }
        if (runeBookValues == null) {
            runeBookValues = new ArrayList<>();
        }
    }

    public static void save() {
        File runeBookFile = new File(RUNEBOOK_FILENAME);
        if (!runeBookFile.exists()) {
            try {
                runeBookFile.createNewFile();
            } catch (IOException e) {
                log.error("Exception occurred while creating rune book file", e);
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(runeBookFile))) {
            dataOutputStream.writeInt(runeBookValues.size());
            for (ChampionBuild p : runeBookValues) {
                p.getRunePage().setChampion(p.getChampion());
                p.getRunePage().setName(p.getName());
                p.getRunePage().setSource(p.getSource());
                p.getRunePage().setSourceName(p.getSourceName());
                p.getRunePage().serialize(dataOutputStream);
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            log.error("Exception occurred while saving rune book file", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while saving rune book file", false);
        }
    }

    public static RunePage getRuneBookPage(String key) {
        return runeBookValues.stream()
                .filter(runePage -> runePage.getName().equalsIgnoreCase(key))
                .findFirst()
                .map(ChampionBuild::getRunePage)
                .orElse(null);
    }

    public static void addRuneBookPage(RunePage page) {
        runeBookValues.add(ChampionBuild.builder(page.copy()).create());
        save();
        EventBus.publish(Runes.RUNE_PAGES_EVENT,
                new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

    public static void removeRuneBookPage(String key) {
        runeBookValues.removeIf(runePage -> runePage.getName().equalsIgnoreCase(key));
        save();
        EventBus.publish(Runes.RUNE_PAGES_EVENT,
                new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

}
