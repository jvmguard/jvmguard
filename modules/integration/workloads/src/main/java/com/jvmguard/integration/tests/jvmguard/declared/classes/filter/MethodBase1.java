package com.jvmguard.integration.tests.jvmguard.declared.classes.filter;

import com.jvmguard.annotation.Inheritance;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.MethodTransaction;

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
