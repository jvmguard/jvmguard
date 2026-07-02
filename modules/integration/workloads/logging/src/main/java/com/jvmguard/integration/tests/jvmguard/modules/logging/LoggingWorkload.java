package com.jvmguard.integration.tests.jvmguard.modules.logging;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() {
        for (int i=0; i<2; i++) {
            test();
            waitForNextConfiguration();
        }
    }

    private void test() {
        BasicConfigurator.configure();
        System.setProperty("org.apache.logging.log4j.level", "INFO");

        for (int i=0; i<5; i++) {
            javaLogging(i);
            log4j1(i);
            log4j2(i);
            logback(i);
        }
    }

    @MethodTransaction
    public void javaLogging(int i) {
        LockSupport.parkNanos(200 * 1000 * 1000);
        Logger javaLogger = Logger.getAnonymousLogger();

        if (i == 1) {
            javaLogger.log(Level.INFO, "info");
        } else if (i == 2) {
            javaLogger.log(Level.WARNING, "warning");
        } else if (i == 3) {
            javaLogger.log(Level.SEVERE, "severe");
        } else if (i == 4) {
            javaLogger.log(Level.SEVERE, "severe");
        }
    }

    @MethodTransaction
    public void log4j1(int i) {
        LockSupport.parkNanos(200 * 1000 * 1000);

        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("test");
        if (i == 1) {
            logger.info("info");
        } else if (i == 2) {
            logger.warn("warn");
        } else if (i == 3) {
            logger.error("error");
        } else if (i == 4) {
            logger.fatal("fatal");
        }
    }

    @MethodTransaction
    public void log4j2(int i) {
        LockSupport.parkNanos(200 * 1000 * 1000);

        org.apache.logging.log4j.Logger logger = LogManager.getLogger("test");
        if (i == 1) {
            logger.info("info l2");
        } else if (i == 2) {
            logger.warn("warn l2");
        } else if (i == 3) {
            logger.error("error l2");
        } else if (i == 4) {
            logger.fatal("fatal l2");
        }
    }

    @MethodTransaction
    public void logback(int i) {
        LockSupport.parkNanos(200 * 1000 * 1000);

        org.slf4j.Logger logger = LoggerFactory.getLogger("test");
        System.out.println(logger.getClass());
        if (i == 1) {
            logger.info("info lb");
        } else if (i == 2) {
            logger.warn("warn lb");
        } else if (i == 3) {
            logger.error("error lb");
        } else if (i == 4) {
            logger.error("fatal lb");
        }
    }
}
