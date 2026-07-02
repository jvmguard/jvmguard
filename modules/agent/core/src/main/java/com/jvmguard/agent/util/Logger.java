package com.jvmguard.agent.util;

import com.jvmguard.agent.base.logging.Subsystem;

public class Logger {
    private static final boolean FAIL_ON_LOG = Boolean.getBoolean("jvmguard.int.failOnLog");

    public static boolean isEnabled(Subsystem subsystem, int level) {
        return LoggingHandler.isEnabled(subsystem, level);
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object v1) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, v1);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object v1, Object v2) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, v1, v2);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object v1, Object v2, Object v3) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, v1, v2, v3);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object v1, Object v2, Object v3, Object v4) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, v1, v2, v3, v4);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object v1, Object v2, Object v3, Object v4, Object v5) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, v1, v2, v3, v4, v5);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String format, Object... values) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, format, values);
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, Object obj) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, String.valueOf(obj));
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, Throwable t) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, t.toString());
            loadStackTrace(subsystem, time, t.getStackTrace());
            while (t != null) {
                t = t.getCause();
                if (t != null) {
                    LoggingHandler.log(subsystem, time, "caused by:");
                    loadStackTrace(subsystem, time, t.getStackTrace());
                }
            }
            checkFail(level);
        }
    }

    public static void log(Subsystem subsystem, int level, boolean time, String message, Throwable t) {
        if (LoggingHandler.isEnabled(subsystem, level)) {
            LoggingHandler.log(subsystem, time, message);
            LoggingHandler.log(subsystem, time, t.toString());
            loadStackTrace(subsystem, time, t.getStackTrace());
            checkFail(level);
        }
    }

    protected static void loadStackTrace(Subsystem subsystem, boolean time, StackTraceElement[] stackTraceElements) {
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            LoggingHandler.log(subsystem, time, stackTraceElement.toString());
        }
    }

    private static void checkFail(int level) {
        if (level == 0 && FAIL_ON_LOG) {
            System.exit(1);
        }
    }
}
