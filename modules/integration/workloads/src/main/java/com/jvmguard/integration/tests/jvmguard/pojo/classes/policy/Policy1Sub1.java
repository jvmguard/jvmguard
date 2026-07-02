package com.jvmguard.integration.tests.jvmguard.pojo.classes.policy;

import java.util.logging.Level;

public class Policy1Sub1 extends Policy1 {
    @Override
    public void m1(int time, Throwable throwable, Level level) throws Throwable {
        super.m1(time, throwable, level);
    }

    @Override
    public void m2(int time, Throwable throwable, Level level) throws Throwable {
        super.m2(time, throwable, level);
    }

}
