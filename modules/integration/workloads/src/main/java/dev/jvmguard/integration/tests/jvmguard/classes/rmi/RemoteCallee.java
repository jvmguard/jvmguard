package dev.jvmguard.integration.tests.jvmguard.classes.rmi;

import dev.jvmguard.integration.util.SleepHelper;

public class RemoteCallee {
    public static void call() throws InterruptedException {
        SleepHelper.sleep(100);
    }
}
