package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class WithSpanService {

    @WithSpan
    public void outer() {
        inner();
    }

    @WithSpan
    public void inner() {
    }

    @WithSpan("custom span name")
    public void named() {
    }

    // @WithSpan wins over @Observed on the same method
    @WithSpan("both annotations")
    @Observed(contextualName = "must not appear")
    public void withSpanAndObserved() {
    }
}
