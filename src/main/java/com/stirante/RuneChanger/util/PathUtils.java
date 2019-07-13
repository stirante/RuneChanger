package com.stirante.RuneChanger.util;

import java.io.File;
import java.net.URISyntaxException;

public class PathUtils {

    public static String getWorkingDirectory() {
        File f = new File(getJarLocation());
        if (!f.exists() || f.isDirectory()) {
            return System.getProperty("user.dir");
        }
        return f.getParentFile().getPath();
    }

    public static String getJarLocation() {
        try {
            return new File(PathUtils.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get the jar location!", e);
        }
    }

    public static File getAssetsDir() {
        return new File(getWorkingDirectory(), "assets");
    }

}
