package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;

import java.util.Locale;
import java.util.ResourceBundle;

public class LangHelper {

    private static ResourceBundle resourceBundle;

    /**
     * Get default resource bundle
     *
     * @return default resource bundle
     */
    @SuppressWarnings("ConstantConditions")
    public static ResourceBundle getLang() {
        if (resourceBundle == null) {
            if (DebugConsts.OVERRIDE_LANGUAGE != null) {
                resourceBundle =
                        ResourceBundle.getBundle("lang.messages", Locale.forLanguageTag(DebugConsts.OVERRIDE_LANGUAGE));
            }
            else if (SimplePreferences.getSettingsValue("force_english") != null &&
                    SimplePreferences.getSettingsValue("force_english").equalsIgnoreCase("true")) {
                resourceBundle =
                        ResourceBundle.getBundle("lang.messages", Locale.ROOT);
            }
            else {
                resourceBundle = ResourceBundle.getBundle("lang.messages");
            }
        }
        return resourceBundle;
    }

}
