package com.jvmguard.integration.tests.jvmguard.matched.classes.naming;

import com.jvmguard.integration.tests.jvmguard.matched.classes.BaseIf1;
import com.jvmguard.integration.util.SleepHelper;

public class Sub1 implements BaseIf1 {
    @Override
    public void b1() {

    }

    @Override
    public void b2() {

    }

    @Override
    public void b3() {
        m2();
    }

    public void m2() {
        SleepHelper.sleep(100);
    }
}
