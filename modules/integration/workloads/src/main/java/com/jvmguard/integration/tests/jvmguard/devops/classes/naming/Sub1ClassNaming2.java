package com.jvmguard.integration.tests.jvmguard.devops.classes.naming;

import com.jvmguard.integration.util.SleepHelper;

public class Sub1ClassNaming2 implements ClassNaming2{
    private String field1 ="sub1f";

    @Override
    public void m1(String param) {
    }

    @Override
    public void m2() {
        SleepHelper.sleep(500);
    }
}
