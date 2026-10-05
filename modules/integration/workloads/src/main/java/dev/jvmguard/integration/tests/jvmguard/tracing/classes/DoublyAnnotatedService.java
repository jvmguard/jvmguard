package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

import dev.jvmguard.annotation.MethodTransaction;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class DoublyAnnotatedService {

    @MethodTransaction
    @WithSpan
    public void bothWays() {
    }
}
