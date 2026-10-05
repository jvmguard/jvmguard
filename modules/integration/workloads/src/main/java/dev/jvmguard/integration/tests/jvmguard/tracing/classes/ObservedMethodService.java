package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

import io.micrometer.observation.annotation.Observed;

public class ObservedMethodService {

    @Observed
    public void observedMethod() {
    }

    @Observed(contextualName = "observed span name")
    public void namedObserved() {
    }
}
