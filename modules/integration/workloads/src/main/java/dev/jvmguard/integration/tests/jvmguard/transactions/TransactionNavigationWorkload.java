package dev.jvmguard.integration.tests.jvmguard.transactions;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.integration.AbstractJvmGuardRun;

public class TransactionNavigationWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<150 * 5; i++) {
            invokeEverything();
        }
        waitForNextConfiguration();
    }

    @MethodTransaction(group = "sample")
    private void invokeEverything() {
        prep1();
        prep2();
    }

    @MethodTransaction
    private void prep2() {
        sleep(200);
    }

    @MethodTransaction
    private void prep1() {
        sleep(200);
    }

}
