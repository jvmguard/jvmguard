package dev.jvmguard.integration.tests.jvmguard.trigger.action;

import dev.jvmguard.integration.AbstractJvmGuardRun;

public class WebhookWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() throws InterruptedException {
        waitForNextConfiguration();
    }
}
