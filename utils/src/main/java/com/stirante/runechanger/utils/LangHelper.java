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

    public static Locale getLocale() {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.FORCE_ENGLISH, false)) {
            return Locale.ROOT;
        }
        else {
            return Locale.getDefault();
        }
    }

}
