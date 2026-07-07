package com.jvmguard.integration.tests.jvmguard.matched.classes.reentry;

import com.jvmguard.annotation.MethodTransaction;

public class Reentry3 {
    public void m1() {
       m2();
    }

    public void m2() {
        m3();
    }

    @MethodTransaction
    private static void m3() {
    }
}
