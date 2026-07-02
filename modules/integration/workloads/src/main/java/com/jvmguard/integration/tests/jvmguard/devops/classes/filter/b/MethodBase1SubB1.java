package com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b;

import com.jvmguard.annotation.NoTransaction;
import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.MethodBase1;

public abstract class MethodBase1SubB1 extends MethodBase1 {
    @Override
    @NoTransaction
    public void m3() {
        throw new Error();
    }
}
