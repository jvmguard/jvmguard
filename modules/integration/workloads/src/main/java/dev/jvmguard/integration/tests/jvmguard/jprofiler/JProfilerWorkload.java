package dev.jvmguard.integration.tests.jvmguard.jprofiler;

import dev.jvmguard.integration.AbstractJvmGuardRun;

public class JProfilerWorkload extends AbstractJvmGuardRun {

    @Override
    public void run() {
        long acc = 0;
        while (true) {
            for (int i = 0; i < 2_000_000; i++) {
                acc += (i * 31L) ^ (acc + i);
            }
            if (acc == Long.MIN_VALUE) {
                System.out.println(acc); // unreachable; prevents dead-code elimination
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
