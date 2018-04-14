package com.stirante.RuneChanger.util;

import java.util.ResourceBundle;

public class LangHelper {

    private static ResourceBundle resourceBundle;

    /**
     * Get default resource bundle
     *
     * @return default resource bundle
     */
    public static ResourceBundle getLang() {
        if (resourceBundle == null) resourceBundle = ResourceBundle.getBundle("lang.messages");
        return resourceBundle;
    }

}
