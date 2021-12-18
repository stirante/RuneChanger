package com.stirante.runechanger.util;

import java.io.File;
import java.net.URISyntaxException;

public class PathUtils {

    public static String getWorkingDirectory() {
        File f = new File(getJarLocation());
        if (!f.exists() || f.isDirectory()) {
            return System.getProperty("user.dir");
        }
        return f.getParentFile().getParentFile().getPath();
    }

    public static String getJarLocation() {
        try {
            return new File(PathUtils.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get the jar location!", e);
        }
    }

    public static String getJarName() {
        try {
            return new File(PathUtils.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getName();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get the jar location!", e);
        }
    }

    public static File getAssetsDir() {
        return new File(getWorkingDirectory(), "assets");
    }

    public static String getWScriptPath() {
        String systemRoot = System.getenv("SystemRoot");
        File f = new File(systemRoot);
        f = new File(f, "System32");
        f = new File(f, "WScript.exe");
        return f.getAbsolutePath();
    }

}
