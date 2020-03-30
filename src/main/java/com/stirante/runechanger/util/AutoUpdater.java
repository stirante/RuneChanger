package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.Settings;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AutoUpdater {
    private static final Logger log = LoggerFactory.getLogger(AutoUpdater.class);
    private static final String DEV_UPDATE_CONFIG = "https://runechanger.stirante.com/dev/dev.xml";
    private static final String STABLE_UPDATE_CONFIG = "https://runechanger.stirante.com/stable/stable.xml";
    private static final int BUFFER_SIZE = 4096;
    private static Configuration configuration = null;

    public static void resetCache() {
        configuration = null;
    }

    /**
     * Checks whether RuneChanger needs an update
     *
     * @return true, if RuneChanger requires an update
     */
    public static boolean needsUpdate() throws IOException {
        if (DebugConsts.isRunningFromIDE()) {
            return false;
        }
        if (!SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_UPDATE, true)) {
            return false;
        }

        Configuration read = getConfiguration();
        return read.requiresUpdate();
    }

    public static String getEstimatedUpdateSize() throws IOException {
        int size = 0;
        Configuration read = getConfiguration();
        for (FileMetadata file : read.getFiles()) {
            if (file.requiresUpdate()) {
                size += file.getSize();
            }
        }
        return humanReadableByteCountSI(size);
    }

    /**
     * From: https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
     */
    public static String humanReadableByteCountSI(long bytes) {
        String s = bytes < 0 ? "-" : "";
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1000L ? bytes + " B"
                : b < 999_950L ? String.format("%s%.1f kB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f MB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f GB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f TB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f PB", s, b / 1e3)
                : String.format("%s%.1f EB", s, b / 1e6);
    }

    public static void performUpdate() {
        try {
            copyDir(new File("image").toPath(), new File("updateImage").toPath());
            FileWriter writer = new FileWriter("update.xml");
            getConfiguration().write(writer);
            writer.flush();
            writer.close();
            Runtime.getRuntime().exec("cmd /c start update.bat");
            System.exit(0);
        } catch (IOException e) {
            log.error("Exception occurred while starting update", e);
            if (Countly.isInitialized()) {
                Countly.session().addCrashReport(e, true);
            }
        }
    }

    /**
     * https://stackoverflow.com/a/10068306/6459649
     */
    private static void copyDir(Path src, Path dest) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(dest.resolve(src.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Configuration getConfiguration() throws IOException {
        if (configuration != null) {
            return configuration;
        }
        String configUrl = STABLE_UPDATE_CONFIG;
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, false)) {
            configUrl = DEV_UPDATE_CONFIG;
        }
        Reader reader = new InputStreamReader(new URL(configUrl).openStream());
        configuration = Configuration.read(reader);
        reader.close();
        return configuration;
    }

    /**
     * From https://stackoverflow.com/a/15135212/6459649
     */
    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir, name)));
        int count;
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
            log.error("Exception occurred while extracting zip file", e);
            if (Countly.isInitialized()) {
                Countly.session().addCrashReport(e, false);
            }
        }
    }

    private static void generateConfig() throws IOException {
        File target = new File("target");
        File[] files = target.listFiles((file, s) -> s.endsWith(".zip"));
        if (files == null || files.length == 0) {
            System.err.println("Zip file not found inside target folder! Skipping config file generation.");
            return;
        }
        File zip = files[0];
        File imageDir = new File("image");
        imageDir.mkdir();
        extract(zip, imageDir);
        Configuration build = Configuration.builder()
                .baseUri("https://runechanger.stirante.com/dev")
                .basePath(new File("").getAbsolutePath())
                .files(FileMetadata.streamDirectory(imageDir.getAbsolutePath())
                        .peek(r -> r.classpath(r.getSource().toString().endsWith(".jar"))))
                .launcher(RuneChanger.class)
                .build();
        FileWriter writer = new FileWriter("image/dev.xml");
        build.write(writer);
        writer.flush();
        writer.close();
        build = Configuration.builder()
                .baseUri("https://runechanger.stirante.com/stable")
                .basePath(new File("").getAbsolutePath())
                .files(FileMetadata.streamDirectory(imageDir.getAbsolutePath())
                        .filter(r -> !r.getSource().toString().endsWith("dev.xml"))
                        .peek(r -> r.classpath(r.getSource().toString().endsWith(".jar"))))
                .launcher(RuneChanger.class)
                .build();
        writer = new FileWriter("image/stable.xml");
        build.write(writer);
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        generateConfig();
        Runtime.getRuntime().exec("explorer.exe \"" + new File("image").getAbsolutePath() + "\"");
    }

    public static void checkUpdate() {
        try {
            if (needsUpdate()) {
                boolean update = Settings.openYesNoDialog(LangHelper.getLang()
                        .getString("update_available"), String.format(LangHelper.getLang()
                        .getString("update_question"), getEstimatedUpdateSize()));
                if (update) {
                    performUpdate();
                }
            }
        } catch (IOException ex) {
            log.error("Exception occurred while checking for update", ex);
        }
    }
}