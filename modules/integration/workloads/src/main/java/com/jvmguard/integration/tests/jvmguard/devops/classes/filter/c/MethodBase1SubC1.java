package com.jvmguard.integration.tests.jvmguard.devops.classes.filter.c;

import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.MethodBase1;

public abstract class MethodBase1SubC1 extends MethodBase1 {
    @Override
    public void m3() {
        throw new Error();
    }
}
