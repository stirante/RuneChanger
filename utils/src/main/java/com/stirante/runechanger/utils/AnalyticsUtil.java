package com.stirante.runechanger.utils;

import com.stirante.eventbus.EventBus;

public class AnalyticsUtil {

    public static void addCrashReport(Throwable t, String comment, boolean fatal) {
        EventBus.publish(CrashReport.NAME, new CrashReport(t, comment, fatal));
    }

    public static class CrashReport {
        public static final String NAME = "AnalyticsCrashReport";

        private final String comment;
        private final boolean fatal;
        private final Throwable t;

        private CrashReport(Throwable t, String comment, boolean fatal) {
            this.t = t;
            this.comment = comment;
            this.fatal = fatal;
        }

        public String getComment() {
            return comment;
        }

        public boolean isFatal() {
            return fatal;
        }

        public Throwable getThrowable() {
            return t;
        }

    }

}
