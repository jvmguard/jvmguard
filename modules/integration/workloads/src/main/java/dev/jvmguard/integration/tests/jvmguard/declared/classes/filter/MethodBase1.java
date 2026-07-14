package dev.jvmguard.integration.tests.jvmguard.declared.classes.filter;

import dev.jvmguard.annotation.Inheritance;
import dev.jvmguard.annotation.Inheritance.Mode;
import dev.jvmguard.annotation.MethodTransaction;

public class MethodBase1 {
    @MethodTransaction(inheritance = @Inheritance(value = Mode.WITH_SUBCLASS_NAMES, filter = "*2"), group = "group1")
    public void m1() {

    }

    @MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUBCLASS_NAMES), group = "group2")
    public void m2() {

    }

    @MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUBCLASS_NAMES), group = "group3")
    public void m3() {

    }
}
