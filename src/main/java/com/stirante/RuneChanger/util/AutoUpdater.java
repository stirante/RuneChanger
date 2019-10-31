package com.stirante.RuneChanger.util;

import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.DebugConsts;
import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.model.github.Asset;
import com.stirante.RuneChanger.model.github.Release;
import com.stirante.RuneChanger.model.github.Version;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class AutoUpdater {

    public static final String UPDATE_JAR_FILENAME = "RuneChangerUpdate.jar";
    public static final String UPDATE_ZIP_FILENAME = "update.zip";
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
        Release latest = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create()
                .fromJson(new InputStreamReader(in), Release.class);
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
            if (!updateScript.delete()) {
                log.error("Failed to delete the update script!");
            }
        }
    }

    /**
     * Checks whether RuneChanger is up to date
     *
     * @return true, if RuneChanger is up to date
     */
    public static boolean check() throws IOException {
        if (DebugConsts.DISABLE_AUTOUPDATE ||
                (SimplePreferences.getSettingsValue("autoUpdate") != null &&
                        SimplePreferences.getSettingsValue("autoUpdate").equals("false"))) {
            return true;
        }
        if (cachedRelease == null) {
            fetchRelease();
        }
        //If jar update date on github is later than build time of current version, then we need to update
        for (Asset asset : cachedRelease.assets) {
            if ((asset.name.endsWith(".jar") || asset.name.endsWith(".zip")) && asset.name.startsWith("RuneChanger-")) {
                log.info("Github version is from " +
                        SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(asset.updatedAt));
                //Build time will always be earlier than publish time, so we check, if those differ by more than 6h
                return asset.updatedAt.getTime() - Version.INSTANCE.buildTime.getTime() < 21600000;
            }
        }
        //If we don't find the jar in latest release (which SHOULD NOT happen), we return, that it's up to date
        return true;
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
            if ((asset.name.endsWith(".jar") || asset.name.endsWith(".zip")) && asset.name.startsWith("RuneChanger-")) {
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
                boolean isZip = fileUrl.endsWith(".zip");
                File currentFile = new File(PathUtils.getJarLocation());
                URL url = new URL(fileUrl);
                HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
                long completeFileSize = httpConnection.getContentLength();
                BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
                FileOutputStream fos = new FileOutputStream(new File(currentFile.getParentFile(), isZip ? UPDATE_ZIP_FILENAME : UPDATE_JAR_FILENAME));
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
                if (!isZip) {
                    Files.write(Paths.get(currentFile.getParentFile()
                                    .getAbsolutePath(), UPDATE_SCRIPT_FILENAME),
                            String.format("@echo off\r\necho %s\r\ntimeout 3\r\ndel \"%s\"\r\nren \"%s\" \"%s\"\r\nstart %s\r\nexit\r\n",
                                    LangHelper.getLang().getString("restart_in_3_seconds"),
                                    currentFile.getAbsolutePath(),
                                    UPDATE_JAR_FILENAME,
                                    MAIN_JAR_FILENAME,
                                    MAIN_JAR_FILENAME
                            ).getBytes());
                    Runtime.getRuntime()
                            .exec("cmd /c start " + UPDATE_SCRIPT_FILENAME, null, currentFile.getParentFile());
                    System.exit(0);
                }
                else {
                    Files.write(Paths.get(currentFile.getParentFile()
                                    .getAbsolutePath(), UPDATE_SCRIPT_FILENAME),
                            String.format("@echo off\r\necho %s\r\ntimeout 3\r\ndel \"%s\"\r\nren \"%s\" \"%s\"\r\ncall start.bat\r\nexit\r\n",
                                    LangHelper.getLang().getString("restart_in_3_seconds"),
                                    currentFile.getAbsolutePath(),
                                    UPDATE_JAR_FILENAME,
                                    MAIN_JAR_FILENAME
                            ).getBytes());
                    Runtime.getRuntime()
                            .exec("cmd /c start " + UPDATE_SCRIPT_FILENAME, null, currentFile.getParentFile());
                    System.exit(0);
                }
            } catch (Exception ignored) {
            }
        };
        new Thread(updateThread).start();
    }

    /**
     * From https://stackoverflow.com/a/10634536/6459649
     */

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        //Very special case, for updating to new updater
        if (name.equalsIgnoreCase(MAIN_JAR_FILENAME)) {
            name = UPDATE_JAR_FILENAME;
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir, name)));
        int count = -1;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.close();
    }

    private static void mkdirs(File outdir, String path) {
        File d = new File(outdir, path);
        if (!d.exists()) {
            d.mkdirs();
        }
    }

    private static String dirpart(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }

    /***
     * Extract zipfile to outdir with complete directory structure
     * @param zipfile Input .zip file
     * @param outdir Output directory
     */
    public static void extract(File zipfile, File outdir) {
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile));
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();
                if (entry.isDirectory()) {
                    mkdirs(outdir, name);
                    continue;
                }
                /* this part is necessary because file entry can come before
                 * directory entry where is file located
                 * i.e.:
                 *   /foo/foo.txt
                 *   /foo/
                 */
                dir = dirpart(name);
                if (dir != null) {
                    mkdirs(outdir, dir);
                }

                extractFile(zin, outdir, name);
            }
            zin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        log.info("Runechanger is up to date: " + check());
        log.info("Forcing update..");
        performUpdate();
    }

}
