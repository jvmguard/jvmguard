package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

import io.micrometer.observation.annotation.Observed;

@Observed
public class ObservedClassService {

    public void classLevelMethod() {
    }
}
