package com.jvmguard.integration.tests.jvmguard.modules.java21.vthread.many;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VThreadManyWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 2; i++) {
            doWork();
        }
    }

    private void doWork() throws InterruptedException {
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            System.out.println("run " + i);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int j = 0; j < 100_000; j++) {
                    executor.submit(this::invokeOne);
                }
            }
            Thread.sleep(1000);
        }
        System.out.println("done after " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime));
        waitForNextConfiguration();
    }

    @MethodTransaction
    private void invokeOne() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
