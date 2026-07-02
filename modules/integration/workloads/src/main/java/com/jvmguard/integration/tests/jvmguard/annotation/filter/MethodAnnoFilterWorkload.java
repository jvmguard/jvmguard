package com.jvmguard.integration.tests.jvmguard.annotation.filter;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.annotation.filter.classes.Class1;
import com.jvmguard.integration.tests.jvmguard.annotation.filter.classes.Class2;

public class MethodAnnoFilterWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<5; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        try {
            Class1.custom1();
        } catch (Throwable ignored) {}
        try {
            Class1.devOps1();
        } catch (Throwable ignored) {}
        try {
            Class1.devOps2();
        } catch (Throwable ignored) {}
        try {
            Class2.custom1();
        } catch (Throwable ignored) {}
        try {
            Class2.devOps1();
        } catch (Throwable ignored) {}
    }
}
