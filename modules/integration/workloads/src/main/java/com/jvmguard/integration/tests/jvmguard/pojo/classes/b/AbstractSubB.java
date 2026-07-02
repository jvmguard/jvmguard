package com.jvmguard.integration.tests.jvmguard.pojo.classes.b;

import com.jvmguard.integration.tests.jvmguard.pojo.classes.BaseIf1;

public abstract class AbstractSubB implements BaseIf1 {
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
