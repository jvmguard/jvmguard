package com.jvmguard.integration.tests.jvmguard.rest;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.util.SleepHelper;

import java.util.concurrent.locks.LockSupport;

public class RestTransactionWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        new Thread() {
            @Override
            public void run() {
                overdue();
            }
        }.start();
        act();
        waitForNextConfiguration();
    }

    private void act() throws InterruptedException {
        transaction1(300);
        transaction1(300);
        for (int i = 0; i < 20; i++) {
            transaction1(150);
        }
    }

    @MethodTransaction(naming = @Part(text = "transaction Щ"))
    public static void transaction1(int waitingTime) {
        transaction2(waitingTime);
    }

    @MethodTransaction(group = "noPolicy")
    public static void transaction2(int waitingTime) {
        SleepHelper.sleep(waitingTime);
    }

    @MethodTransaction
    public static void overdue() {
        overdueInner();
    }

    @MethodTransaction
    public static void overdueInner() {
        while (true) {
            LockSupport.park();
        }
    }

}
