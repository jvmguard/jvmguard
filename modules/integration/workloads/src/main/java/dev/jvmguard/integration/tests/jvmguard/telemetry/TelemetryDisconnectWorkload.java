package dev.jvmguard.integration.tests.jvmguard.telemetry;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.util.SleepHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelemetryDisconnectWorkload extends AbstractJvmGuardRun {
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    protected void work() throws InterruptedException {
        while (true) {
            act();
        }
    }

    private void act() throws InterruptedException {
        List<Callable<Object>> callables = new ArrayList<>();
        for (int callableNo=0; callableNo<10; callableNo++) {
            callables.add(() -> {
                for (int i = 0; i < 20; i++) {
                    transaction1(1000);
                }
                for (int i = 0; i < 1100; i++) {
                    transaction1(200);
                }
                return null;
            });
        }
        executor.invokeAll(callables);
        System.out.println("returned");
    }

    @Telemetry(value = "custom1")
    private static int getVmNoTelemetry() {
        return 2;
    }


    @MethodTransaction
    public static void transaction1(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }

}
