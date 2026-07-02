package com.jvmguard.integration.tests.jvmguard.annotation.filter.classes;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.tests.jvmguard.annotation.classes.naming.MAnno1;

public class Class2 {
    @MethodTransaction(group="test")
    public static void devOps1() {
        throw new RuntimeException();
    }

    @MAnno1
    public static void custom1() {
        throw new RuntimeException();
    }
}
