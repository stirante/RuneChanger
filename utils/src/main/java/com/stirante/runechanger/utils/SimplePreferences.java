package com.stirante.runechanger.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

public class SimplePreferences {
    private static final Logger log = LoggerFactory.getLogger(SimplePreferences.class);

    private static final String SETTINGS_FILENAME =
            new File(PathUtils.getWorkingDirectory(), "RuneChangerSettings.dat").getAbsolutePath();
    private static HashMap<String, String> settingsValues;

    public static void load() {
        File settingsValuesFile = new File(SETTINGS_FILENAME);
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
                AnalyticsUtil.addCrashReport(e, "Exception occurred while loading settings file", false);
            }
        }
        if (settingsValues == null) {
            settingsValues = new HashMap<>();
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
            AnalyticsUtil.addCrashReport(e, "Exception occurred while saving settings file", false);
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

    public static class SettingsKeys {
        public static final String ANTI_AWAY = "antiAway";
        public static final String AUTO_SYNC = "autoSync";
        public static final String SMART_DISENCHANT = "smartDisenchant";
        public static final String CHAMPION_SUGGESTIONS = "championSuggestions";
        public static final String SMART_CHAMPION_SUGGESTIONS = "smartChampionSuggestions";
        public static final String RESTART_ON_DODGE = "restartOnDodge";
        public static final String APPLY_SUMMONER_SPELLS = "applySummonerSpells";
        public static final String FLASH_FIRST = "flashFirst";

        public static final String FORCE_ENGLISH = "forceEnglish";
        public static final String AUTO_UPDATE = "autoUpdate";
        public static final String EXPERIMENTAL_CHANNEL = "devChannel";
        public static final String ALWAYS_ON_TOP = "alwaysOnTop";
        public static final String ANALYTICS = "analytics";
        public static final String ENABLE_ANIMATIONS = "enableAnimations";
        public static final String RUN_AS_ADMIN = "runAsAdmin";
        public static final String AUTO_EXIT = "autoExit";
        public static final String EMERGENCY_SHORTCUTS = "emergencyShortcuts";

        public static final String AUTO_MESSAGE = "autoMessage";
        public static final String AUTO_MESSAGE_TEXT = "autoMessageText";
        public static final String CUSTOM_MESSAGE = "customMessage";
        public static final String CUSTOM_MESSAGE_TEXT = "customMessageText";

        public static final String FORCE_QUICK_REPLIES = "forceQuickReplies";
        public static final String QUICK_REPLIES = "quickReplies";
        public static final String ADC_MESSAGE = "adcMsg";
        public static final String SUPP_MESSAGE = "suppMsg";
        public static final String MID_MESSAGE = "midMsg";
        public static final String JUNGLE_MESSAGE = "jungleMsg";
        public static final String TOP_MESSAGE = "topMsg";
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
