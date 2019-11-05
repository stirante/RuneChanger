package com.stirante.RuneChanger.util;

import com.google.gson.Gson;
import com.stirante.RuneChanger.model.LeagueSettings;
import com.stirante.RuneChanger.model.RunePage;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SimplePreferences {

    private static final String SETTINGS_FILENAME = new File(PathUtils.getConfigDir(), "RuneChangerSettings.dat").getAbsolutePath();
    private static final String RUNEBOOK_FILENAME = new File(PathUtils.getConfigDir(), "RuneChangerRuneBook.dat").getAbsolutePath();
    private static final String LEAGUESETTINGS_FILENAME = new File(PathUtils.getConfigDir(), "RuneChangerLeagueSettings.dat").getAbsolutePath();
    private static ArrayList<RunePage> runeBookValues;
    private static HashMap<String, String> settingsValues;
    private static HashMap<String, String> leagueSettings;

    public static ArrayList<RunePage> getRuneBookValues() {
        return runeBookValues;
    }

    public static void load() {
        File settingsValuesFile = new File(SETTINGS_FILENAME);
        File runeBookValuesFile = new File(RUNEBOOK_FILENAME);
        File runechangerLeagueSettingsFile = new File(LEAGUESETTINGS_FILENAME);

        settingsValues = loadHashmapValues(settingsValuesFile, settingsValues);
        leagueSettings = loadHashmapValues(runechangerLeagueSettingsFile, leagueSettings);

        try (DataInputStream ois = new DataInputStream(new FileInputStream(runeBookValuesFile))) {
            runeBookValues = new ArrayList<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                RunePage p = new RunePage();
                p.deserialize(ois);
                runeBookValues.add(p);
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
        }
        if (runeBookValues == null) {
            runeBookValues = new ArrayList<>();
        }
    }

    public static void save() {
        File runechangerSettings = new File(SETTINGS_FILENAME);
        File runechangerLeagueSettings = new File(LEAGUESETTINGS_FILENAME);

        createConfigFile(runechangerSettings);
        createConfigFile(runechangerLeagueSettings);
        saveHashmapValues(runechangerSettings, settingsValues);
        saveHashmapValues(runechangerLeagueSettings, leagueSettings);

        File runeBookFile = new File(RUNEBOOK_FILENAME);
        if (!runeBookFile.exists()) {
            try {
                runeBookFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(runeBookFile))) {
            dataOutputStream.writeInt(runeBookValues.size());
            for (RunePage p : runeBookValues) {
                p.serialize(dataOutputStream);
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getSettingsValue(String key) {
        return settingsValues.get(key);
    }

    public static boolean settingsContainsKey(String key) {
        return settingsValues.containsKey(key);
    }

    public static void addSettingsElement(String key, String value) {
        settingsValues.put(key, value);
        save();
    }

    public static void removeLeagueSettingsElement(String key) {
        leagueSettings.remove(key);
    }

    public static LeagueSettings getLeagueSettingsValue(String key) {
        LeagueSettings settings = new Gson().fromJson(leagueSettings.get(key), LeagueSettings.class);
        return settings;
    }

    public static boolean leagueSettingsContainsKey(String key) {
        return leagueSettings.containsKey(key);
    }

    public static void addLeagueSettingsElement(LeagueSettings value) {
        String jsonValue = new Gson().toJson(value);
        if (leagueSettingsContainsKey(value.getIdentifier())) {
            removeLeagueSettingsElement(value.getIdentifier());
        }

        leagueSettings.put(value.getIdentifier(), jsonValue);
        save();
    }

    public static RunePage getRuneBookPage(String key) {
        return runeBookValues.stream()
                .filter(runePage -> runePage.getName().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public static void addRuneBookPage(RunePage page) {
        runeBookValues.add(page);
        save();
    }

    public static void removeRuneBookPage(String key) {
        runeBookValues.removeIf(runePage -> runePage.getName().equalsIgnoreCase(key));
        save();
    }

    private static void saveHashmapValues(File file, HashMap hashmap) {
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(hashmap.size());
            for (Object key : hashmap.keySet()) {
                dataOutputStream.writeUTF(key.toString());
                dataOutputStream.writeUTF(hashmap.get(key).toString());
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap loadHashmapValues(File file, HashMap hashmap) {
        try (DataInputStream ois = new DataInputStream(new FileInputStream(file))) {
            hashmap = new HashMap<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                hashmap.put(key, value);
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
        }

        if (hashmap == null) {
            hashmap = new HashMap();
        }

        return hashmap;
    }

    private static void createConfigFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
