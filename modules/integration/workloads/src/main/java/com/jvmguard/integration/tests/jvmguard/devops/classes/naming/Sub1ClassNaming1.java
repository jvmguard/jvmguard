package com.jvmguard.integration.tests.jvmguard.devops.classes.naming;

import com.jvmguard.integration.util.SleepHelper;

public class Sub1ClassNaming1 extends ClassNaming1 {
    @Override
    public void m1(int time) {
        SleepHelper.sleep(time);
    }

    @Override
    public void innerM2(int time) {
        SleepHelper.sleep(time);
    }

    @Override
    public void m2(int time) {
        innerM2(time);
    }

    @Override
    protected void innerM3(int time) {
        SleepHelper.sleep(time);
    }

    @Override
    public void m3(int time) {
        innerM3(time);
    }
}
