package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.gui.Constants;
import com.sun.jna.platform.win32.Advapi32Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

public class AutoStartUtils {
    private static final Logger log = LoggerFactory.getLogger(AutoStartUtils.class);

    private static final String REGISTRY_AUTOSTART_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    public static boolean isAutoStartEnabled() {
        return Advapi32Util.registryValueExists(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
    }

    public static void checkAutoStartPath() {
        if (DebugConsts.isRunningFromIDE()) {
            return;
        }
        if (isAutoStartEnabled()) {
            String s =
                    Advapi32Util.registryGetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
            if (!s.equals(getStartCommand())) {
                Advapi32Util.registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME, getStartCommand());
                try {
                    prepareScript();
                } catch (IOException e) {
                    log.error("Exception occurred while checking auto start path", e);
                }
            }
        }
    }

    public static void setAutoStart(boolean enabled) {
        boolean isEnabled = isAutoStartEnabled();
        if (isEnabled == enabled) {
            return;
        }
        if (enabled) {
            try {
                prepareScript();
            } catch (IOException e) {
                log.error("Exception occurred while setting auto start", e);
            }
            Advapi32Util.registryCreateKey(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY);
            Advapi32Util.registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME, getStartCommand());
        }
        else {
            Advapi32Util.registryDeleteValue(HKEY_CURRENT_USER, REGISTRY_AUTOSTART_KEY, Constants.APP_NAME);
        }
    }

    public static void prepareScript() throws IOException {
        File f = new File("open.bat").getAbsoluteFile();
        FileInputStream fis = new FileInputStream(f);
        String s = StringUtils.streamToString(fis);
        fis.close();
        String[] lines = s.split("\n");
        StringBuilder sb = new StringBuilder(lines[0]);
        sb.append("\ncd /d \"").append(f.getParentFile().getAbsolutePath()).append("\"");
        for (int i = 1; i < lines.length; i++) {
            sb.append("\n").append(lines[i]);
        }
        sb.append(" -minimized");
        File file = new File(f.getParentFile(), "autostart.bat");
        FileWriter fw = new FileWriter(file);
        fw.write(sb.toString());
        fw.flush();
        fw.close();
    }

    public static String getStartCommand() {
        return "\"" + PathUtils.getWScriptPath() + "\" \"" + PathUtils.getWorkingDirectory() + "\\silent.vbs\" \"" +
                PathUtils.getWorkingDirectory() + "\\autostart.bat\"";
    }

}
