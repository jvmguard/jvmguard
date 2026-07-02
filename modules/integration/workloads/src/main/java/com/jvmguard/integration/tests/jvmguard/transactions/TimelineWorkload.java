package com.jvmguard.integration.tests.jvmguard.transactions;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.util.SleepHelper;

public class TimelineWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        while (true) {
            invokeEverything(false);
            invokeEverything(true);
        }
    }

    @MethodTransaction(group = "sample")
    private void invokeEverything(boolean slow) {
        prep1(slow);
        prep2(slow);
    }

    @MethodTransaction
    private void prep2(boolean slow) {
        SleepHelper.sleep(slow ? 300 : 100);
    }

    @MethodTransaction
    private void prep1(boolean slow) {
        SleepHelper.sleep(slow ? 300 : 100);
    }

}
