package com.stirante.runechanger.util;

import com.stirante.eventbus.EventBus;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.model.client.RunePage;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SimplePreferences {
    private static final Logger log = LoggerFactory.getLogger(SimplePreferences.class);

    private static final String SETTINGS_FILENAME =
            new File(PathUtils.getWorkingDirectory(), "RuneChangerSettings.dat").getAbsolutePath();
    private static final String RUNEBOOK_FILENAME =
            new File(PathUtils.getWorkingDirectory(), "RuneChangerRuneBook.dat").getAbsolutePath();
    private static ArrayList<RunePage> runeBookValues;
    private static HashMap<String, String> settingsValues;

    public static ArrayList<RunePage> getRuneBookValues() {
        return runeBookValues;
    }

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
                log.error("Exception occurred while loading settings file", e);
                if (Countly.isInitialized()) {
                    Countly.session().addCrashReport(e, false);
                }
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
                log.error("Exception occurred while loading rune book file", e);
                if (Countly.isInitialized()) {
                    Countly.session().addCrashReport(e, false);
                }
            }
        }
        if (runeBookValues == null) {
            runeBookValues = new ArrayList<>();
        }
    }

    public static void save() {
        File prefs = new File(SETTINGS_FILENAME);
        if (!prefs.exists()) {
            try {
                prefs.createNewFile();
            } catch (IOException e) {
                log.error("Exception occurred while creating settings file", e);
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
            log.error("Exception occurred while saving settings file", e);
            if (Countly.isInitialized()) {
                Countly.session().addCrashReport(e, false);
            }
        }


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
            for (RunePage p : runeBookValues) {
                p.serialize(dataOutputStream);
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            log.error("Exception occurred while saving rune book file", e);
            if (Countly.isInitialized()) {
                Countly.session().addCrashReport(e, false);
            }
        }

    }

    public static boolean getBooleanValue(String key, boolean def) {
        String val = settingsValues.getOrDefault(key, Boolean.toString(def));
        return Boolean.parseBoolean(val) || val.equals("1");
    }

    public static String getStringValue(String key, String def) {
        return settingsValues.getOrDefault(key, def);
    }

    public static boolean containsKey(String key) {
        return settingsValues.containsKey(key);
    }

    public static void putBooleanValue(String key, boolean value) {
        settingsValues.put(key, Boolean.toString(value));
        save();
    }

    public static void putStringValue(String key, String value) {
        settingsValues.put(key, value);
        save();
    }

    public static RunePage getRuneBookPage(String key) {
        return runeBookValues.stream()
                .filter(runePage -> runePage.getName().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public static void addRuneBookPage(RunePage page) {
        runeBookValues.add(page.copy());
        save();
        EventBus.publish(ClientEventListener.RunePagesEvent.NAME,
                new ClientEventListener.RunePagesEvent(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

    public static void removeRuneBookPage(String key) {
        runeBookValues.removeIf(runePage -> runePage.getName().equalsIgnoreCase(key));
        save();
        EventBus.publish(ClientEventListener.RunePagesEvent.NAME,
                new ClientEventListener.RunePagesEvent(ClientEventListener.WebSocketEventType.UPDATE, null));
    }

    public static class SettingsKeys {
        public static final String ANTI_AWAY = "antiAway";
        public static final String AUTO_ACCEPT = "autoAccept";
        public static final String QUICK_REPLIES = "quickReplies";
        public static final String FORCE_ENGLISH = "forceEnglish";
        public static final String AUTO_UPDATE = "autoUpdate";
        public static final String EXPERIMENTAL_CHANNEL = "devChannel";
        public static final String ALWAYS_ON_TOP = "alwaysOnTop";
        public static final String AUTO_SYNC = "autoSync";
        public static final String SMART_DISENCHANT = "smartDisenchant";
        public static final String CHAMPION_SUGGESTIONS = "championSuggestions";
        public static final String ANALYTICS = "analytics";
        public static final String ENABLE_ANIMATIONS = "enableAnimations";
        public static final String RESTART_ON_DODGE = "restartOnDodge";
    }

    public static class AnalyticsKeys {
        public static final String USER_ID = "userId";
    }

    public static class InternalKeys {
        public static final String CLIENT_PATH = "clientPath";
        public static final String DONATE_DONT_ASK = "donateDontAsk";
        public static final String ASKED_ANALYTICS = "askedAnalytics";
    }

    public static class FlagKeys {
        public static final String CREATED_SHORTCUTS = "createdShortcuts";
        public static final String UPDATED_LOGO = "updatedLogo";
    }

}
