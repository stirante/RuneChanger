package com.stirante.runechanger.utils;

import java.util.*;

public class LangHelper {
    private static final Set<String> RTL;

    static {
        RTL = Set.of("ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "sd", "ug", "ur", "yi");
    }

    public static boolean isTextRTL(Locale locale) {
        return RTL.contains(locale.getLanguage());
    }

    public static Locale getLocale(List<Locale> available) {
        // Handle old setting, that used to be a switch between English and system language
        if (!SimplePreferences.containsKey(SimplePreferences.SettingsKeys.LANGUAGE) && SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.FORCE_ENGLISH, false)) {
            SimplePreferences.putStringValue(SimplePreferences.SettingsKeys.LANGUAGE, Locale.ENGLISH.toLanguageTag());
            SimplePreferences.removeKey(SimplePreferences.SettingsKeys.FORCE_ENGLISH);
        }
        if (SimplePreferences.containsKey(SimplePreferences.SettingsKeys.LANGUAGE)) {
            return Locale.forLanguageTag(SimplePreferences.getStringValue(SimplePreferences.SettingsKeys.LANGUAGE, Locale.ENGLISH.toLanguageTag()));
        }
        else {
            Locale defLocale = Locale.getDefault();
            if (available.contains(defLocale)) {
                return defLocale;
            }
            else {
                String lang = defLocale.getLanguage();
                String country = defLocale.getCountry();
                Locale matching = available.stream()
                        .filter(locale -> locale.getLanguage().equals(lang) && locale.getCountry().equals(country))
                        .findFirst()
                        .orElse(null);
                if (matching != null) {
                    return matching;
                }
                matching = available.stream()
                        .filter(locale -> locale.getLanguage().equals(lang))
                        .findFirst()
                        .orElse(null);
                if (matching != null) {
                    return matching;
                }
                return Locale.ENGLISH;
            }
        }
    }

}
