package com.jvmguard.integration.tests.jvmguard.declared.classes.filter.c;

public class CMethodBase1Sub3 extends MethodBase1SubC1 {
    @Override
    public void m1() {
        throw new RuntimeException();
    }

    @Override
    public void m2() {
        throw new RuntimeException();
    }
}
