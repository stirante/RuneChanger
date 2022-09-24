package com.stirante.runechanger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.stirante.runechanger.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DebugConsts {
    //Used for testing the UI
    public static final boolean MOCK_SESSION = false;
    public static final boolean DISPLAY_FAKE = false;
    public static final boolean FORCE_TEAM_COMP_ANALYSIS = false;

    //those are really annoying while testing
    public static final boolean DISABLE_NOTIFICATIONS = false;

    //logs every event (except for voice) to System.out
    public static final boolean PRINT_EVENTS = false;
    public static final boolean PRINT_EVENTS_DATA = false;

    //enables extra info from analytics library
    public static final boolean ENABLE_ANALYTICS_DEBUG = false;

    //dynamically refreshes stylesheet on save
    public static final boolean ENABLE_STYLESHEET_DEBUG = false;

    /**
     * If instead of a JAR we have a folder, it is very likely, that RuneChanger is running from an IDE
     */
    public static boolean isRunningFromIDE() {
        return new File(PathUtils.getJarLocation()).isDirectory();
    }

    public static void enableDebugMode() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> appender = logger.getAppender("STDOUT");
        appender.clearAllFilters();
        appender.addFilter(new Filter<>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                if (Level.DEBUG.isGreaterOrEqual(event.getLevel()) &&
                        !event.getLoggerName().startsWith("com.stirante")) {
                    return FilterReply.DENY;
                }
                return FilterReply.ACCEPT;
            }
        });
        logger.setLevel(Level.DEBUG);
    }

}
