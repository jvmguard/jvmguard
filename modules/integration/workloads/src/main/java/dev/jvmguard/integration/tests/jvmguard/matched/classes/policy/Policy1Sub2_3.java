package dev.jvmguard.integration.tests.jvmguard.matched.classes.policy;

import java.util.logging.Level;

public class Policy1Sub2_3 extends Policy1Sub2 {
    @Override
    public void m1(int time, Throwable throwable, Level level) throws Throwable {
        super.m1(time, throwable, level);
    }

    @Override
    public void m2(int time, Throwable throwable, Level level) throws Throwable {
        super.m2(time, throwable, level);
    }

}
