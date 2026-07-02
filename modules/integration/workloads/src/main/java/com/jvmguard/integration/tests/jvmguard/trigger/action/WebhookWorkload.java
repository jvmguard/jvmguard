package com.jvmguard.integration.tests.jvmguard.trigger.action;

import com.jvmguard.integration.AbstractJvmGuardRun;

public class WebhookWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() throws InterruptedException {
        waitForNextConfiguration();
    }
}
