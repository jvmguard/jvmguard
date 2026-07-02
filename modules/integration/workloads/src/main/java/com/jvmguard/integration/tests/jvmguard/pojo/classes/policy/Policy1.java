package com.jvmguard.integration.tests.jvmguard.pojo.classes.policy;

import com.jvmguard.integration.tests.jvmguard.pojo.classes.TestPolicyHelper;

import java.util.logging.Level;

public class Policy1 {
    public void m1(int time, Throwable throwable, Level level) throws Throwable {
        TestPolicyHelper.handle(time, throwable, level);
    }

    public void m2(int time, Throwable throwable, Level level) throws Throwable {
        TestPolicyHelper.handle(time, throwable, level);
    }

}
