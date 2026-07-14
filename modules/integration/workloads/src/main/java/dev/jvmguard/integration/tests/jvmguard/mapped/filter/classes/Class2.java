package dev.jvmguard.integration.tests.jvmguard.mapped.filter.classes;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno1;

public class Class2 {
    @MethodTransaction(group="test")
    public static void declared1() {
        throw new RuntimeException();
    }

    @MAnno1
    public static void custom1() {
        throw new RuntimeException();
    }
}
