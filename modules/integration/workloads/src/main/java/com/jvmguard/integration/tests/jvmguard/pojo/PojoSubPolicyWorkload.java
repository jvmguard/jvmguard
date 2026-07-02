package com.jvmguard.integration.tests.jvmguard.pojo;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1Sub1;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1Sub2_4_$$_test;

import java.util.logging.Level;

public class PojoSubPolicyWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        invokeEverything();
        waitForNextConfiguration();

        invokeEverything();
        waitForNextConfiguration();
    }

    private void invokeEverything() {
        invokeFully(new Policy1());
        invokeFully(new Policy1Sub1());
        invokeFully(new Policy1Sub2_4_$$_test());
    }

    private void invokeFully(Policy1 policy1) {
        invoke(policy1, 400, null, Level.INFO);
        for (int i=0; i<10; i++) {
            invoke(policy1, 50, null, Level.INFO);
        }
        invoke(policy1, 200, null, Level.WARNING);
        invoke(policy1, 200, new RuntimeException(), Level.WARNING);
        invoke(policy1, 2500, null, Level.INFO);
    }

    private void invoke(Policy1 policy1, int time, Throwable throwable, Level level) {
        try {
            policy1.m1(time, throwable, level);
        } catch (Throwable ignored) {
        }
        try {
            policy1.m2(time, throwable, level);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
            case 3:
                return new String[0];
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
