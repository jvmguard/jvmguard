package dev.jvmguard.integration.tests.jvmguard.mapped.filter;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.mapped.filter.classes.Class1;
import dev.jvmguard.integration.tests.jvmguard.mapped.filter.classes.Class2;

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
            Class1.declared1();
        } catch (Throwable ignored) {}
        try {
            Class1.declared2();
        } catch (Throwable ignored) {}
        try {
            Class2.custom1();
        } catch (Throwable ignored) {}
        try {
            Class2.declared1();
        } catch (Throwable ignored) {}
    }
}
