package dev.jvmguard.integration.tests.jvmguard.matched.classes.b;

import dev.jvmguard.integration.tests.jvmguard.matched.classes.BaseIf1;

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
