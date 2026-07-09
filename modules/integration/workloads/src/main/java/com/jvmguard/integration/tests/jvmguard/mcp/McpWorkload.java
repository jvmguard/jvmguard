package com.jvmguard.integration.tests.jvmguard.mcp;

import com.jvmguard.annotation.*;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.util.SleepHelper;

public class McpWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            transaction1(500);
        }
    }

    @Telemetry(value = "cpu")
    private static int getCpuTelemetry() {
        return 50;
    }

    @MethodTransaction(naming = @Part(text = "MCP transaction"))
    public static void transaction1(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }
}
