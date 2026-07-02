package com.jvmguard.integration.tests.jvmguard.transactions;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.util.SleepHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NestedOverdueWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        while (true) {
            act();
            waitForNextConfiguration();
        }
    }

    private void act() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i=0; i<5; i++) {
            executor.submit(new Runnable() {
                @Override public void run() {
                    policy1();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    @MethodTransaction(group = "policy1")
    public void policy1() {
        policy2();
    }

    @MethodTransaction(group = "policy2")
    public void policy2() {
        SleepHelper.sleep(1000 * 5);
    }

}
