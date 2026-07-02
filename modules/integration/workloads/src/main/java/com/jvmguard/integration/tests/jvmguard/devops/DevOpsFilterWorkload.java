package com.jvmguard.integration.tests.jvmguard.devops;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.MethodBase1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub2;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub3;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub2;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub3;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub2;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub3;

public class DevOpsFilterWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<5; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<2; i++) {
            call(new AMethodBase1Sub1());
            call(new AMethodBase1Sub2());
            call(new AMethodBase1Sub3());

            call(new BMethodBase1Sub1());
            call(new BMethodBase1Sub2());
            call(new BMethodBase1Sub3());

            call(new CMethodBase1Sub1());
            call(new CMethodBase1Sub2());
            call(new CMethodBase1Sub3());
        }
    }

    private void call(MethodBase1 methodBase1) {
        try {
            methodBase1.m1();
        } catch (RuntimeException ignored) {
        }
        try {
            methodBase1.m2();
        } catch (RuntimeException ignored) {
        }
        try {
            methodBase1.m3();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.MethodBase1SubA1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub2"};
            case 3:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub2",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.MethodBase1SubB1"};
            case 4:
                return new String[0];
            case 5:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.MethodBase1SubC1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub2",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub2",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.BMethodBase1Sub2",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.MethodBase1SubA1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c.CMethodBase1Sub1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.MethodBase1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a.AMethodBase1Sub3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b.MethodBase1SubB1"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
