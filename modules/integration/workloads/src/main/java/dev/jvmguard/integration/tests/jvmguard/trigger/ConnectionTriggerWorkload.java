package dev.jvmguard.integration.tests.jvmguard.trigger;

import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.Util;

public class ConnectionTriggerWorkload extends AbstractJvmGuardRun {
    private static final int VM_NO = Integer.getInteger(Util.VMNO_PROP_NAME, 1);

    @Telemetry(value = "continuous")
    private static int tel1() {
        switch (VM_NO) {
            case 1:
                return 30;
            case 2:
                return 20;
            case 3:
                return 10;
            case 4:
                return 40;
            case 5:
                return 50;
            case 6:
                return 60;
            case 7:
                return 50;
            case 8:
                return 40;
            case 9:
                return 30;
            case 10:
                return 20;
        }
        System.exit(1);
        return 0;
    }
}
