package com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a;

import com.jvmguard.integration.tests.jvmguard.devops.classes.filter.MethodBase1;

public abstract class MethodBase1SubA1 extends MethodBase1 {
    @Override
    public void m3() {
        throw new Error();
    }
}
