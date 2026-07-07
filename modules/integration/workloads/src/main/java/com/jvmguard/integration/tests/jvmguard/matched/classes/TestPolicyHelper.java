package com.jvmguard.integration.tests.jvmguard.matched.classes;

import com.jvmguard.integration.util.SleepHelper;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TestPolicyHelper {
    private final static Logger LOGGER = Logger.getLogger("default");
    static {
        LOGGER.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                return false;
            }
        });
    }

    public static void log(Level level) {
        if (level != null) {
            LOGGER.log(level, "log message");
        }
    }

    public static void handle(int time, Throwable exception, Level level) throws Throwable {
        SleepHelper.sleep(time);
        log(level);
        if (exception != null) {
            throw exception;
        }
    }

    public static void handleUnchecked(int time, RuntimeException exception, Level level) {
        try {
            handle(time, exception, level);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
