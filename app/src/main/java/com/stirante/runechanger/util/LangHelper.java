package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class LangHelper {
    private static final Set<String> RTL;

    static {
        RTL = Set.of("ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "sd", "ug", "ur", "yi");
    }

    private static ResourceBundle resourceBundle;

    /**
     * Get default resource bundle
     *
     * @return default resource bundle
     */
    public static ResourceBundle getLang() {
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle("lang.messages", getLocale());
        }
        return resourceBundle;
    }

    public static boolean isTextRTL() {
        return RTL.contains(getLang().getLocale().getLanguage());
    }

    @SuppressWarnings("ConstantConditions")
    public static Locale getLocale() {
        if (DebugConsts.OVERRIDE_LANGUAGE != null) {
            return Locale.forLanguageTag(DebugConsts.OVERRIDE_LANGUAGE);
        }
        else if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.FORCE_ENGLISH, false)) {
            return Locale.ROOT;
        }
        else {
            return Locale.getDefault();
        }
    }

}
