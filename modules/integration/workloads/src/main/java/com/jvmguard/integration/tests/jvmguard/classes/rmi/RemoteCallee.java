package com.jvmguard.integration.tests.jvmguard.classes.rmi;

import com.jvmguard.integration.util.SleepHelper;

public class RemoteCallee {
    public static void call() throws InterruptedException {
        SleepHelper.sleep(100);
    }
}
