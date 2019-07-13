package com.stirante.RuneChanger.util;

import com.stirante.RuneChanger.gui.Constants;
import com.sun.jna.platform.win32.Advapi32Util;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

public class AutoStartUtils {

    private static final String REGISTRY_AUTOSTART_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    public static boolean isAutoStartEnabled() {
        return Advapi32Util.registryValueExists(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
    }

    public static void checkAutoStartPath() {
        if (isAutoStartEnabled()) {
            String s = Advapi32Util.registryGetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
            String value = "\"" + PathUtils.getJavawPath() + "\" -jar " + PathUtils.getJarLocation() + " -minimized";
            if (!s.equals(value)) {
                Advapi32Util.registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME, value);
            }
        }
    }

    public static void setAutoStart(boolean enabled) {
        boolean isEnabled = isAutoStartEnabled();
        if (isEnabled == enabled) {
            return;
        }
        if (enabled) {
            String value = "\"" + PathUtils.getJavawPath() + "\" -jar " + PathUtils.getJarLocation() + " -minimized";
            Advapi32Util.registryCreateKey(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY);
            Advapi32Util.registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME, value);
        }
        else {
            Advapi32Util.registryDeleteValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
        }
    }

}
