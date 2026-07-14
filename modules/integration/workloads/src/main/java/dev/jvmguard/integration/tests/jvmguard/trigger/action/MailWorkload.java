package dev.jvmguard.integration.tests.jvmguard.trigger.action;

import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.integration.AbstractJvmGuardRun;

public class MailWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() throws InterruptedException {
        waitForNextConfiguration();
    }

    @Telemetry("test1")
    public static int getTest1() {
        return 1000;
    }
}
