package com.jvmguard.integration.tests.jvmguard.mapped.filter.classes;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno1;

public class Class1 {
    @MethodTransaction(group="test")
    public static void declared1() {
        throw new RuntimeException();
    }

    @MethodTransaction(group="test2")
    public static void declared2() {
        throw new RuntimeException();
    }

    @MAnno1
    public static void custom1() {
        throw new RuntimeException();
    }
}
