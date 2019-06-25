package com.stirante.RuneChanger.util;

import com.google.gson.Gson;
import com.stirante.RuneChanger.DebugConsts;
import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.model.github.Asset;
import com.stirante.RuneChanger.model.github.Release;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AutoUpdater {

    public static final String UPDATE_JAR_FILENAME = "RuneChangerUpdate.jar";
    public static final String MAIN_JAR_FILENAME = "RuneChanger.jar";
    public static final String UPDATE_SCRIPT_FILENAME = "update.bat";
    private static Release cachedRelease;

    /**
     * Fetches latest release info from GitHub
     */
    private static void fetchRelease() throws IOException {
        URL url = new URL(Constants.LATEST_RELEASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream in = conn.getInputStream();
        Release latest = new Gson().fromJson(new InputStreamReader(in), Release.class);
        in.close();
        cachedRelease = latest;
    }

    /**
     * Cleans up update script
     */
    public static void cleanup() throws Exception {
        File currentFile = new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File updateScript = new File(currentFile.getParentFile(), UPDATE_SCRIPT_FILENAME);
        if (updateScript.exists()) {
            updateScript.delete();
        }
    }

    /**
     * Checks whether RuneChanger is up to date
     *
     * @return true, if RuneChanger is up to date
     */
    public static boolean check() throws IOException {
        if (DebugConsts.DISABLE_AUTOUPDATE ||
                (SimplePreferences.getValue("autoUpdate") != null &&
                        SimplePreferences.getValue("autoUpdate").equals("true"))) {
            return true;
        }
        if (cachedRelease == null) {
            fetchRelease();
        }
        return cachedRelease.tagName.equalsIgnoreCase(Constants.VERSION_STRING);
    }

    /**
     * Performs update:
     * 1. Download new jar file with progress bar
     * 2. Prepare script, which removes old version, renames new to old name and starts RuneChanger
     * 3. Run script
     * 4. Stop RuneChanger
     */
    public static void performUpdate() throws IOException {
        if (cachedRelease == null) {
            fetchRelease();
        }
        for (Asset asset : cachedRelease.assets) {
            if (asset.name.endsWith(".jar") && asset.name.startsWith("RuneChanger-")) {
                downloadUpdate(asset.browserDownloadUrl);
            }
        }
    }

    /**
     * Downloads update with progress and continues update process.
     * From https://stackoverflow.com/a/22273319/6459649
     */
    private static void downloadUpdate(String fileUrl) {
        final JProgressBar jProgressBar = new JProgressBar();
        jProgressBar.setMaximum(100000);
        jProgressBar.setStringPainted(true);
        JFrame frame = new JFrame();
        frame.setContentPane(jProgressBar);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 70);
        frame.setResizable(false);
        frame.setTitle(LangHelper.getLang().getString("updating"));
        frame.setType(Window.Type.UTILITY);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);

        Runnable updateThread = () -> {
            try {
                File currentFile =
                        new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                URL url = new URL(fileUrl);
                HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
                long completeFileSize = httpConnection.getContentLength();
                BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
                FileOutputStream fos = new FileOutputStream(new File(currentFile.getParentFile(), UPDATE_JAR_FILENAME));
                BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;
                    final int currentProgress =
                            (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100000d);
                    SwingUtilities.invokeLater(() -> jProgressBar.setValue(currentProgress));
                    bout.write(data, 0, x);
                }
                bout.close();
                in.close();
                Files.write(Paths.get(currentFile.getParentFile()
                                .getAbsolutePath(), UPDATE_SCRIPT_FILENAME),
                        String.format("@echo off\r\necho %s\r\ntimeout 3\r\ndel \"%s\"\r\nren \"%s\" \"%s\"\r\nstart %s\r\nexit\r\n",
                                LangHelper.getLang().getString("restart_in_3_seconds"),
                                currentFile.getAbsolutePath(),
                                UPDATE_JAR_FILENAME,
                                MAIN_JAR_FILENAME,
                                MAIN_JAR_FILENAME
                        ).getBytes());
                Runtime.getRuntime().exec("cmd /c start " + UPDATE_SCRIPT_FILENAME, null, currentFile.getParentFile());
                System.exit(0);
            } catch (Exception ignored) {
            }
        };
        new Thread(updateThread).start();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Is up to date: " + check());
        System.out.println("Forcing update");
        performUpdate();
    }

}
