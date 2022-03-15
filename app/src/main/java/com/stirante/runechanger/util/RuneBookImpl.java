package com.stirante.runechanger.util;

import com.stirante.eventbus.EventBus;
import com.stirante.runechanger.api.RuneBook;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.client.Runes;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RunePage;
import com.stirante.runechanger.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RuneBookImpl implements RuneBook {
    private static final Logger log = LoggerFactory.getLogger(RuneBookImpl.class);

    private static final String RUNEBOOK_FILENAME =
            new File(PathUtils.getWorkingDirectory(), "RuneChangerRuneBook.dat").getAbsolutePath();
    private final List<ChampionBuild> runeBookValues;

    public RuneBookImpl(List<ChampionBuild> runeBookValues) {
        this.runeBookValues = runeBookValues;
    }

    public static RuneBook loadRuneBook(RuneChangerApi api) {
        List<ChampionBuild> runeBookValues = new ArrayList<>();
        File runeBookValuesFile = new File(RUNEBOOK_FILENAME);
        try (DataInputStream ois = new DataInputStream(new FileInputStream(runeBookValuesFile))) {
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                RunePage p = new RunePage();
                p.deserialize(api, ois);
                runeBookValues.add(ChampionBuild.builder(p).create());
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                log.error("Exception occurred while loading rune book file", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while loading rune book file", false);
            }
        }
        return new RuneBookImpl(runeBookValues);
    }

    public void save() {
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

    public List<ChampionBuild> getRuneBookValues() {
        return runeBookValues;
    }

    public RunePage getRuneBookPage(String key) {
        return runeBookValues.stream()
                .filter(runePage -> runePage.getName().equalsIgnoreCase(key))
                .findFirst()
                .map(ChampionBuild::getRunePage)
                .orElse(null);
    }

    public void addRuneBookPage(RunePage page) {
        runeBookValues.add(ChampionBuild.builder(page.copy()).create());
        save();
        EventBus.publish(Runes.RUNE_PAGES_EVENT,
                new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

    public void removeRuneBookPage(String key) {
        runeBookValues.removeIf(runePage -> runePage.getName().equalsIgnoreCase(key));
        save();
        EventBus.publish(Runes.RUNE_PAGES_EVENT,
                new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

}
