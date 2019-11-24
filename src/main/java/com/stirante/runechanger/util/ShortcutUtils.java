package com.stirante.runechanger.util;

import mslinks.ShellLink;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ShortcutUtils {

    public static void createShortcut(File directory, String linkName, String fileName) throws IOException {
        String dir = PathUtils.getWorkingDirectory();
        File iconFile = new File(dir + File.pathSeparator + "icon.ico");
        if (!iconFile.exists()) {
            InputStream iconStream =
                    ShortcutUtils.class.getResourceAsStream("/images/runechanger-runeforge-icon-32x32.ico");
            byte[] bytes = iconStream.readAllBytes();
            iconStream.close();
            Files.write(iconFile.toPath(), bytes);
        }
        ShellLink sl = ShellLink.createLink(fileName);
        sl.setIconLocation(iconFile.getAbsolutePath());
        sl.setWorkingDir(dir);
        sl.setName(linkName);
        sl.saveTo(new File(directory, linkName + ".lnk").getAbsolutePath());
    }

    public static void createMenuShortcuts() throws IOException {
        File menuFolder = new File(System.getenv("AppData") + "\\Microsoft\\Windows\\Start Menu\\Programs\\RuneChanger");
        menuFolder.mkdir();
        createShortcut(menuFolder, "RuneChanger", "open.bat");
        createShortcut(menuFolder, "RuneChanger (Debug)", "run.bat");
    }

    public static void createDesktopShortcut() throws IOException {
        File folder = FileSystemView.getFileSystemView().getHomeDirectory();
        createShortcut(folder, "RuneChanger", "open.bat");
    }

}
