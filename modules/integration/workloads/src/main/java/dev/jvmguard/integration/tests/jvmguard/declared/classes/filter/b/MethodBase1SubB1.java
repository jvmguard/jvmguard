package dev.jvmguard.integration.tests.jvmguard.declared.classes.filter.b;

import dev.jvmguard.annotation.NoTransaction;
import dev.jvmguard.integration.tests.jvmguard.declared.classes.filter.MethodBase1;

public abstract class MethodBase1SubB1 extends MethodBase1 {
    @Override
    @NoTransaction
    public void m3() {
        throw new Error();
    }
}
