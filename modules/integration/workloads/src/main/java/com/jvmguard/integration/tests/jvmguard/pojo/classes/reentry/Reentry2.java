package com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry;

import com.jvmguard.annotation.MethodTransaction;

public class Reentry2 {
    public static void m2() {
        m3();
    }

    @MethodTransaction(group = "group1")
    public static void m3() {
        m4(3);
    }

    public static void m4(int i) {
        if (i > 0) {
            m4(i-1);
        }
    }
}
