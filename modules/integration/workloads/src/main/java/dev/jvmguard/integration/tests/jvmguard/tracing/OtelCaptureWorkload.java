package dev.jvmguard.integration.tests.jvmguard.tracing;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.FakeOtelLogs;
import dev.jvmguard.integration.tests.jvmguard.tracing.classes.OtelCaptureService;

public class OtelCaptureWorkload extends AbstractJvmGuardRun {

    static {
        FakeOtelLogs.register();
    }

    @Override
    protected void work() {
        OtelCaptureService service = new OtelCaptureService();
        for (int i = 0; i < 30; i++) {
            service.slowOperation();
        }
        waitForNextConfiguration();
    }
}
