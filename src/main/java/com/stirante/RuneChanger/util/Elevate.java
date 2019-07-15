package com.stirante.RuneChanger.util;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import lombok.extern.slf4j.Slf4j;

/**
 * Source: https://stackoverflow.com/questions/30082838/elevate-java-application-while-running
 */
@Slf4j
public class Elevate {

    /**
     * The program argument indicating the need of being elevated
     */
    private static final String ELEVATE_ARG = "-elevate";

    /**
     * If requested, elevates the Java process started with the given arguments to administrator level.
     *
     * @param args The Java program arguments
     * @return The cleaned program arguments
     */
    public static String[] elevate(String[] args) {

        // Check for elevation marker.
        boolean elevate = false;
        if (args.length > 0) {
            elevate = args[args.length - 1].equals(ELEVATE_ARG);
        }
        log.info("Elevate: " + elevate);
        if (elevate) {
            // Get the command and remove the elevation marker.
            String command = System.getProperty("sun.java.command");
            command = command.replace(ELEVATE_ARG, "");

            // Get class path and default java home.
            String classPath = System.getProperty("java.class.path");
            String javaHome = System.getProperty("java.home");
            String vm = javaHome + "\\bin\\java.exe";

            // Check for alternate VM for elevation. Full path to the VM may be passed with: -Delevation.vm=...
            if (System.getProperties().contains("elevation.vm")) {
                vm = System.getProperty("elevation.vm");
            }
            String parameters = "-cp " + classPath;
            parameters += " " + command;
            Shell32.INSTANCE.ShellExecute(null, "runas", vm, parameters, null, 0);

            int lastError = Kernel32.INSTANCE.GetLastError();
            if (lastError != 0) {
                String errorMessage = Kernel32Util.formatMessageFromLastErrorCode(lastError);
                errorMessage += "\n  vm: " + vm;
                errorMessage += "\n  parameters: " + parameters;
                throw new IllegalStateException("Error performing elevation: " + lastError + ": " + errorMessage);
            }
            System.exit(0);
        }
        return args;
    }
}
