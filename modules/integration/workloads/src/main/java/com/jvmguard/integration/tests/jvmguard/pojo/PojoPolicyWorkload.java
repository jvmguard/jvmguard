package com.jvmguard.integration.tests.jvmguard.pojo;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.TestPolicyHelper;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.*;
import com.jvmguard.integration.util.SleepHelper;

import java.io.IOError;
import java.io.IOException;
import java.util.logging.Level;

public class PojoPolicyWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        try {
            TestPolicyHelper.handle(100, new IOError(new RuntimeException()), Level.INFO);
            TestPolicyHelper.handle(100, new IOError(new RuntimeException()), Level.INFO);
            TestPolicyHelper.handle(100, new IOError(new RuntimeException()), Level.INFO);
        } catch (Throwable ignored) {
        }

        invokeEverything(true);
        waitForNextConfiguration();

        invokeEverything(false);
        waitForNextConfiguration();

        normalOnly();
        waitForNextConfiguration();
    }

    private void normalOnly() {
        try {
            for (int i=0; i<50; i++) {
                invoke(new Policy1(), 1, null);
                invoke(new Policy1Sub1(), 1, null);
                invoke(new Policy1Sub2(), 1, null);
                invoke(new Policy1Sub2_2(), 1, null);
                invoke(new Policy1Sub2_3(), 1, null);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void invokeEverything(boolean firstTime) {
        invokeFast(new Policy1());
        invokeFast(new Policy1Sub1());
        invokeFast(new Policy1Sub2());
        if (firstTime) {
            SleepHelper.sleep(1000 * 70);
        }
        invokeFully(new Policy1());
        invokeFully(new Policy1Sub1());
        invokeFully(new Policy1Sub2());
        invokeException(new Policy1Sub2_2(), new IOException());
        invokeException(new Policy1Sub2_3(), new IOException());
    }
    private void invokeFast(Policy1 policy1) {
        try {
            for (int i=0; i<50; i++) {
                invoke(policy1, 1, null);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void invokeFully(Policy1 policy1) {
        try {
            invoke(policy1, 3, null);
            invoke(policy1, 7, null);
            invoke(policy1, 15, null);
            invoke(policy1, 55, null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        invokeException(policy1, new IOError(new RuntimeException()));
        invokeException(policy1, new LinkageError());
        invokeException(policy1, new RuntimeException());
        invokeException(policy1, new NullPointerException());
        invokeException(policy1, new IOException());
        invokeException(policy1, new IllegalAccessException());

        try {
            policy1.m1(100, null, Level.INFO);
            policy1.m2(100, null, Level.INFO);
            policy1.m1(100, null, Level.WARNING);
            policy1.m2(100, null, Level.WARNING);
            policy1.m1(100, null, Level.SEVERE);
            policy1.m2(100, null, Level.SEVERE);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void invoke(Policy1 policy1, int multiplier, Level level) throws Throwable {
        policy1.m1(30 * multiplier, null, level);
        policy1.m2(100 * multiplier, null, level);
    }

    private void invokeException(Policy1 policy1, Throwable throwable) {
        try {
            policy1.m1(100, throwable, null);
        } catch (Throwable ignored) {
        }
        try {
            policy1.m2(100, throwable, null);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1Sub1" ,"com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1"};
            case 3:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1Sub2_2"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
