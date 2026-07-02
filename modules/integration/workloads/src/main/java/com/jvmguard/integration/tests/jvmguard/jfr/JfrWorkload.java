package com.jvmguard.integration.tests.jvmguard.jfr;

import com.jvmguard.integration.AbstractJvmGuardRun;

import java.util.concurrent.locks.LockSupport;

public class JfrWorkload extends AbstractJvmGuardRun {
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            LockSupport.park();
        }
    }
}
