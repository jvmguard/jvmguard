package com.jvmguard.integration.tests.jvmguard.devops.classes.filter.a;

public class AMethodBase1Sub3 extends MethodBase1SubA1 {
    @Override
    public void m1() {
        throw new RuntimeException();
    }

    @Override
    public void m2() {
        throw new RuntimeException();
    }
}
