package com.jvmguard.integration.tests.jvmguard.dashboard;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.Util;
import com.jvmguard.integration.util.SleepHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VmDataWorkload extends AbstractJvmGuardRun {
    private static final int VM_NO = Integer.getInteger(Util.VMNO_PROP_NAME, 1);

    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    protected void work() throws InterruptedException {
        if (getVmNo() > 1) {
            sleep(1000 * 80);
            act();
        }
        waitForNextConfiguration();
    }

    private void act() throws InterruptedException {
        List<Callable<Object>> callables = new ArrayList<>();
        for (int callableNo=0; callableNo<10; callableNo++) {
            callables.add(new Callable<Object>() {
                @Override
                public Object call() {
                    for (int i = 0; i < 20; i++) {
                        transaction1(1000);
                    }
                    for (int i = 0; i < 1100; i++) {
                        transaction1(200);
                    }
                    return null;
                }
            });
        }
        executor.invokeAll(callables);
        System.out.println("returned");
    }

    @Telemetry(value = "vmNoSingle")
    private static int getVmNoTelemetry() {
        return VM_NO;
    }

    @Telemetry(value = "vmNoGroup", line = "normal")
    private static int getVmNoTelemetry2() {
        return VM_NO;
    }

    @Telemetry(value = "vmNoGroup", line = "double")
    private static int getVmNoTelemetry3() {
        return VM_NO * 2;
    }


    @MethodTransaction
    public static void transaction1(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }

}
