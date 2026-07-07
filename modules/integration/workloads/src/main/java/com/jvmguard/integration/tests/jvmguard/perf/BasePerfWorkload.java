package com.jvmguard.integration.tests.jvmguard.perf;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno1;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasePerfWorkload extends AbstractJvmGuardRun {

    @Override
    public void run() {
        try {
            work();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void work() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();

        final long repetition = Long.getLong("jvmguard.benchmark.repetition", 50_000_000L);
        final int threads = 4;

        Callable<Object> normal = () -> {
            for (long i = 0; i < repetition; i++) {
                singleExecute();
            }
            return null;
        };
        // Warm up on one thread so the agent has received its configuration and instrumented singleExecute
        // before the timed portion runs.
        executorService.submit(normal).get();

        Collection<Callable<Object>> callables = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            callables.add(normal);
        }

        saveHeapInfo("start", false);
        startTiming();
        long start = System.currentTimeMillis();
        executorService.invokeAll(callables);
        long elapsed = System.currentTimeMillis() - start;
        saveTiming("finish");
        saveHeapInfo("finish", true);
        try {
            savePerformance("def");
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
        System.out.println("BENCHMARK RESULT: " + threads + " threads x " + repetition
            + " instrumented singleExecute() calls = " + elapsed + " ms");
        System.out.println("finished everything");
        System.exit(0);
    }

    @MethodTransaction
    private void singleExecute() {
    }

    @SuppressWarnings("unused")
    private static void pojoExecute() {
    }

    @MAnno1
    @SuppressWarnings("unused")
    private static void annoExecute() {
    }
}
