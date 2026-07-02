package com.jvmguard.integration.tests.jvmguard.devops.classes.filter.b;

public class BMethodBase1Sub3 extends MethodBase1SubB1 {
    @Override
    public void m1() {
        throw new RuntimeException();
    }

    @Override
    public void m2() {
        throw new RuntimeException();
    }
}
