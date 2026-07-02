package com.jvmguard.integration.tests.jvmguard.pojo.classes.a;

import com.jvmguard.integration.tests.jvmguard.pojo.classes.BaseIf1;

public abstract class AbstractSubA implements BaseIf1 {
    @Override
    public void b1() {
        throw new RuntimeException();
    }

    @Override
    public void b2() {

    }

    public void a1() {

    }
}
