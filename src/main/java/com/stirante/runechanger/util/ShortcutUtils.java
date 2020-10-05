package com.stirante.runechanger.util;

import mslinks.ShellLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ShortcutUtils {
    private static final Logger log = LoggerFactory.getLogger(ShortcutUtils.class);

    public static void createShortcut(File directory, String linkName, String fileName) throws IOException {
        String dir = PathUtils.getWorkingDirectory();
        File iconFile = new File(dir + File.separator + "icon.ico");
        ShellLink sl = ShellLink.createLink(fileName);
        sl.setIconLocation(iconFile.getAbsolutePath());
        sl.setWorkingDir(dir);
        sl.setName(linkName);
        String absolutePath = new File(directory, linkName + ".lnk").getAbsolutePath();
        sl.saveTo(absolutePath);
        log.info(String.format("Created shortcut for %s in %s", fileName, absolutePath));
    }

    public static void createMenuShortcuts() throws IOException {
        File menuFolder =
                new File(System.getenv("AppData") + "\\Microsoft\\Windows\\Start Menu\\Programs\\RuneChanger");
        menuFolder.mkdir();
        createShortcut(menuFolder, "RuneChanger", "open.bat");
        createShortcut(menuFolder, "RuneChanger (Debug)", "run.bat");
    }

    public static void createDesktopShortcut() throws IOException {
        File folder = FileSystemView.getFileSystemView().getHomeDirectory();
        createShortcut(folder, "RuneChanger", "open.bat");
    }

}
