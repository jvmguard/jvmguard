package dev.jvmguard.integration.tests.jvmguard.tracing;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.DoublyAnnotatedService;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.ObservedClassService;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.ObservedMethodService;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.WithSpanService;

public class WithSpanWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i = 0; i < 2; i++) {
            WithSpanService withSpanService = new WithSpanService();
            withSpanService.outer();
            withSpanService.named();
            withSpanService.withSpanAndObserved();
            ObservedMethodService observedMethodService = new ObservedMethodService();
            observedMethodService.observedMethod();
            observedMethodService.namedObserved();
            new ObservedClassService().classLevelMethod();
            new DoublyAnnotatedService().bothWays();
        }
        waitForNextConfiguration();
    }
}
