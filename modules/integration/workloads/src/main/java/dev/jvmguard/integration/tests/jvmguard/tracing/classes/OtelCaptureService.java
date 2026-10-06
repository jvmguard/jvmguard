package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

import dev.jvmguard.annotation.MethodTransaction;

public class OtelCaptureService {

    @MethodTransaction
    public void slowOperation() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
