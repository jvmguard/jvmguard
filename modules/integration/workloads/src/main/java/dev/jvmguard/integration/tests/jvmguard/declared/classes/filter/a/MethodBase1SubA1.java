package dev.jvmguard.integration.tests.jvmguard.declared.classes.filter.a;

import dev.jvmguard.integration.tests.jvmguard.declared.classes.filter.MethodBase1;

public abstract class MethodBase1SubA1 extends MethodBase1 {
    @Override
    public void m3() {
        throw new Error();
    }
}
