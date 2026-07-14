package dev.jvmguard.agent.util;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.AgentProperties;
import dev.jvmguard.agent.JvmGuardAgent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.IllegalFormatException;

public class LoggingHandler {
    private static boolean ENABLED = AgentProperties.getBoolean("logEnabled", true);

    private static PrintWriter pw;
    private static final EnumMap<Subsystem, Integer> subsystemToLevel;
    private static File logFile;

    static {
        subsystemToLevel = new EnumMap<>(Subsystem.class);
        for (Subsystem subsystem : Subsystem.values()) {
            Integer level = AgentProperties.getInteger("log" + subsystem.getPropertySuffix(), 0);
            subsystemToLevel.put(subsystem, level);
        }
    }

    public static void setName(String name) {
        String customFile = AgentProperties.getProperty("logFile");
        if (customFile != null) {
            logFile = new File(customFile);
        } else {
            File logDir = new File(JvmGuardAgent.getJvmGuardUserDir(), "log");
            logFile = new File(logDir, name + ".log");
        }
        logFile.getParentFile().mkdirs();
    }

    public static File getLogFile() {
        return logFile;
    }

    public static synchronized void setLogFile(File logFile) {
        LoggingHandler.logFile = logFile;
    }

    public static boolean isEnabled(Subsystem subsystem, int level) {
        return level <= subsystemToLevel.get(subsystem);
    }

    public static synchronized void log(Subsystem subsystem, boolean time, String str) {
        if (!ENABLED) {
            return;
        }
        try {
            if (pw == null) {
                pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logFile)), true);
            }
            pw.print(subsystem);
            if (time) {
                pw.print('[');
                pw.print(new Date());
                pw.print("] ");
            }
            pw.println(str);
        } catch (Throwable e) {
            System.err.println(str);
            e.printStackTrace();
        }
    }

    public static synchronized void log(Subsystem subsystem, boolean time, String format, Object... values) {
        if (!ENABLED) {
            return;
        }
        try {
            if (pw == null) {
                pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logFile)), true);
            }
            pw.print(subsystem);
            if (time) {
                pw.print('[');
                pw.print(new Date());
                pw.print("] ");
            }
            try {
                pw.printf(format, values);
            } catch (IllegalFormatException e) {
                pw.println(e);
                pw.print(format);
                pw.println(Arrays.toString(values));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
