package com.jvmguard.integration.tests.jvmguard.declared.classes.filter.b;

public class BMethodBase1Sub2 extends MethodBase1SubB1 {
    @Override
    public void m1() {
        throw new RuntimeException();
    }

    @Override
    public void m2() {
        throw new RuntimeException();
    }
}
