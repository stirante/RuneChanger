package com.stirante.RuneChanger.util;

import com.stirante.RuneChanger.model.RunePage;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SimplePreferences {

    private static final String SETTINGS_FILENAME = new File(PathUtils.getWorkingDirectory(), "RuneChangerSettings.dat").getAbsolutePath();
    private static final String RUNEBOOK_FILENAME = new File(PathUtils.getWorkingDirectory(), "RuneChangerRuneBook.dat").getAbsolutePath();
    public static ArrayList<RunePage> runeBookValues;
    private static HashMap<String, String> settingsValues;

    public static void load() {
        File settingsValuesFile = new File(SETTINGS_FILENAME);
        File runeBookValuesFile = new File(RUNEBOOK_FILENAME);
        try (DataInputStream ois = new DataInputStream(new FileInputStream(settingsValuesFile))) {
            settingsValues = new HashMap<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                settingsValues.put(key, value);
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
        }
        if (settingsValues == null) {
            settingsValues = new HashMap<>();
        }

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

    public static String getValue(String key) {
        return settingsValues.get(key);
    }

    public static RunePage getRuneBookPage(String key) {
        return runeBookValues.stream()
                .filter(runePage -> runePage.getName().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public static boolean containsKey(String key) {
        return settingsValues.containsKey(key);
    }

    public static void putValue(String key, String value) {
        settingsValues.put(key, value);
        save();
    }

    public static void addRuneBookPage(RunePage page) {
        runeBookValues.add(page);
        save();
    }

    public static void removeRuneBookPage(String key) {
        runeBookValues.removeIf(runePage -> runePage.getName().equalsIgnoreCase(key));
        save();
    }

    public static void save() {
        File prefs = new File(SETTINGS_FILENAME);
        if (!prefs.exists()) {
            try {
                prefs.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(prefs))) {
            dataOutputStream.writeInt(settingsValues.size());
            for (String key : settingsValues.keySet()) {
                dataOutputStream.writeUTF(key);
                dataOutputStream.writeUTF(settingsValues.get(key));
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


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


}
