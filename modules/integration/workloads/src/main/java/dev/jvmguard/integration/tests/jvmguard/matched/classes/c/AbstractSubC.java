package dev.jvmguard.integration.tests.jvmguard.matched.classes.c;

import dev.jvmguard.integration.tests.jvmguard.matched.classes.BaseIf1;

public abstract class AbstractSubC implements BaseIf1 {
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
