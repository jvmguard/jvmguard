package com.jvmguard.integration.tests.jvmguard.trigger.action;

import com.jvmguard.annotation.Telemetry;
import com.jvmguard.integration.AbstractJvmGuardRun;

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
