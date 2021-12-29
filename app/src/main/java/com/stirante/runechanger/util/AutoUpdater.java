package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.utils.FxUtils;
import com.stirante.runechanger.utils.PathUtils;
import com.stirante.runechanger.utils.SimplePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AutoUpdater {
    private static final Logger log = LoggerFactory.getLogger(AutoUpdater.class);
    private static final String DEV_UPDATE_CONFIG = "https://runechanger.stirante.com/dev/dev.xml";
    private static final String STABLE_UPDATE_CONFIG = "https://runechanger.stirante.com/stable/stable.xml";
    private static final int BUFFER_SIZE = 4096;
    public static final String LOCAL_UPDATE_CONFIG = "update.xml";
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
            copyDir(new File("lib").toPath(), new File("updateLib").toPath());
            FileWriter writer = new FileWriter(LOCAL_UPDATE_CONFIG);
            getConfiguration().write(writer);
            writer.flush();
            writer.close();
            Runtime.getRuntime().exec("wscript silent.vbs update.bat");
            System.exit(0);
        } catch (IOException e) {
            log.error("Exception occurred while starting update", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while starting update", true);
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
            AnalyticsUtil.addCrashReport(e, "Exception occurred while extracting zip file", false);
        }
    }

    private static File generateConfig(String path) throws IOException {
        File target = new File(path, "target");
        System.out.println(target.getAbsolutePath());
        File[] files = target.listFiles((file, s) -> s.endsWith(".zip"));
        if (files == null || files.length == 0) {
            log.error("Zip file not found inside target folder! Skipping config file generation.");
            return null;
        }
        File zip = files[0];
        File imageDir = new File(path, "image");
        imageDir.mkdir();
        extract(zip, imageDir);
        Configuration build = Configuration.builder()
                .baseUri("https://runechanger.stirante.com/dev")
                .basePath(new File("").getAbsolutePath())
                .files(FileMetadata.streamDirectory(imageDir.getAbsolutePath())
                        .peek(r -> r.classpath(r.getSource().toString().endsWith(".jar")).ignoreBootConflict()))
                .launcher(RuneChanger.class)
                .property("flavour", "dev")
                .build();
        FileWriter writer = new FileWriter(new File(imageDir, "dev.xml"));
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
                .property("flavour", "stable")
                .build();
        writer = new FileWriter(new File(imageDir, "stable.xml"));
        build.write(writer);
        writer.flush();
        writer.close();
        return imageDir;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.error("No arguments provided! Please provide a path to the folder with a zip.");
            return;
        }
        File imageDir = generateConfig(args[0]);
        if (imageDir != null) {
            Runtime.getRuntime().exec("explorer.exe \"" + imageDir.getAbsolutePath() + "\"");
        }
    }

    public static void checkUpdate() {
        try {
            if (needsUpdate()) {
                String estimatedUpdateSize = getEstimatedUpdateSize();
                FxUtils.doOnFxThread(() -> {
                    boolean update = Settings.openYesNoDialog(RuneChanger.getInstance().getLang()
                            .getString("update_available"), String.format(RuneChanger.getInstance().getLang()
                            .getString("update_question"), estimatedUpdateSize));
                    if (update) {
                        performUpdate();
                    }
                });
            }
        } catch (IOException ex) {
            log.error("Exception occurred while checking for update", ex);
        }
    }

    public static void updateCleanup() {
        try {
            deleteDirectory(new File("updateImage"));
            deleteDirectory(new File("updateLib"));
            new File(LOCAL_UPDATE_CONFIG).delete();
        } catch (IOException e) {
            log.warn("Exception occurred while cleaning up after update", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while cleaning up after update", false);
        }
    }

    public static void deleteOldLibs() {
        try {
            Reader reader = new InputStreamReader(new FileInputStream(LOCAL_UPDATE_CONFIG));
            Configuration configuration = Configuration.read(reader);
            reader.close();
            List<String> acceptableFiles = configuration.getFiles().stream()
                    .map(FileMetadata::getPath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            Files.walk(new File("lib").getAbsoluteFile().getParentFile().toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String relative = path.toString().replace(PathUtils.getWorkingDirectory(), "");
                        return relative.startsWith(File.separator + "image") ||
                                relative.startsWith(File.separator + "lib");
                    })
                    .filter(path -> acceptableFiles.stream()
                            .noneMatch(s -> path.toString().contains(s) || s.contains(path.toString())))
                    .map(Path::toFile)
                    .peek(file -> System.out.println("Deleting " + file.toString()))
                    .forEach(File::deleteOnExit);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    }
                    else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }
}