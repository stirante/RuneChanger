package com.stirante.runechanger.util;

import com.stirante.runechanger.DebugConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DebugUtils {

    public static void measure(Runnable runnable) {
        if (!DebugConsts.isRunningFromIDE()) {
            runnable.run();
            return;
        }
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        long duration;
        try {
            runnable.run();
            duration = System.currentTimeMillis() - start;
            memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory;
        } catch (Throwable t) {
            duration = System.currentTimeMillis() - start;
            memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory;
        }
        StackTraceElement callingStackElement = getCallingStackElement();
        Logger logger = LoggerFactory.getLogger(toClass(callingStackElement));
        if (callingStackElement != null) {
            logger.debug("Executed " + callingStackElement.getFileName() + ":" + callingStackElement.getLineNumber());
            String line = getLine(callingStackElement);
            if (line != null) {
                logger.debug(line);
            }
        }
        logger.debug("Execution took " + duration + "ms.");
        logger.debug("During execution " + memory + " bytes were used");
    }

    private static String getLine(StackTraceElement element) {
        String className = element.getClassName();
        String fileName =
                "src/main/java/" + (className.substring(0, className.lastIndexOf('.') + 1).replace('.', '/')) +
                        element.getFileName();
        File f = new File(fileName);
        try {
            String s = Files.readString(f.toPath());
            return s.split("\n")[element.getLineNumber() - 1].trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Class<?> toClass(StackTraceElement e) {
        if (e == null) {
            return DebugUtils.class;
        }
        try {
            return Thread.currentThread().getContextClassLoader().getParent().loadClass(e.getClassName());
        } catch (ClassNotFoundException ignored) {
        }
        return DebugUtils.class;
    }

    private static StackTraceElement getCallingStackElement() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(DebugUtils.class.getName()) &&
                    ste.getClassName().indexOf("java.lang.Thread") != 0) {
                return ste;
            }
        }
        return null;
    }

}
