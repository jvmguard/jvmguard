package com.jvmguard.integration.tests.jvmguard.annotation;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.annotation.classes.*;

public class AnnotationWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<2; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<2; i++) {
            call(new IncAnno1Sub1());
            call(new IncAnno1());
            call(new Anno2());
            call(new Anno3());
            call(new IncAnno3Sub1());
        }
    }

    private void call(Anno3 anno2) {
        try {
            anno2.m1();
        } catch (RuntimeException ignored) {
        }
        try {
            anno2.m2();
        } catch (RuntimeException ignored) {
        }
        try {
            anno2.m3();
        } catch (Throwable ignored) {
        }
        try {
            anno2.m4();
        } catch (Throwable ignored) {
        }
    }

    private void call(Anno2 anno2) {
        try {
            anno2.m1();
        } catch (RuntimeException ignored) {
        }
        try {
            anno2.m2();
        } catch (RuntimeException ignored) {
        }
        try {
            anno2.m3();
        } catch (Throwable ignored) {
        }
        try {
            anno2.m4();
        } catch (Throwable ignored) {
        }
    }

    private void call(IncAnno1 anno1) {
        try {
            anno1.m1();
        } catch (RuntimeException ignored) {
        }
        try {
            anno1.m2();
        } catch (RuntimeException ignored) {
        }
        try {
            anno1.m3();
        } catch (Throwable ignored) {
        }
        try {
            anno1.m4();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.annotation.classes.IncAnno1",
                    "com.jvmguard.integration.tests.jvmguard.annotation.classes.IncAnno1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.annotation.classes.IncAnno3Sub1"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
