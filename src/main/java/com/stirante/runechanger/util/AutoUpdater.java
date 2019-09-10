package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AutoUpdater {
    private static final String UPDATE_CONFIG = "https://s3.amazonaws.com/runechanger.stirante.com/latest/update.xml";

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

        Configuration read = getConfiguration();
        return !read.requiresUpdate();
    }

    public static String getEstimatedUpdateSize() throws IOException {
        int size = 0;
        Configuration read = getConfiguration();
        for (FileMetadata file : read.getFiles()) {
            if (file.requiresUpdate()) {
                size += file.getSize();
            }
        }
        return humanReadableByteCount(size, false);
    }

    /**
     * From https://stackoverflow.com/a/3758880/6459649
     *
     * @param bytes
     * @param si
     * @return
     */
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void performUpdate() {
        try {
            Runtime.getRuntime().exec("cmd /c start update.bat");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Configuration configuration = null;

    private static Configuration getConfiguration() throws IOException {
        if (configuration != null) {
            return configuration;
        }
        Reader reader = new InputStreamReader(new URL(UPDATE_CONFIG).openStream());
        configuration = Configuration.read(reader);
        reader.close();
        return configuration;
    }

    /**
     * From https://stackoverflow.com/a/10634536/6459649
     */

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
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
                .baseUri("https://s3.amazonaws.com/runechanger.stirante.com/latest")
                .basePath(new File("").getAbsolutePath())
                .files(FileMetadata.streamDirectory(imageDir.getAbsolutePath())
                        .peek(r -> r.classpath(r.getSource().toString().endsWith(".jar"))))
                .launcher(RuneChanger.class)
                .build();
        FileWriter writer = new FileWriter("image/update.xml");
        build.write(writer);
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        generateConfig();
    }

}
