package com.jvmguard.integration.tests.jvmguard.declared.classes.naming;

public class Sub1ClassNaming7 extends ClassNaming7 {
    @Override
    public void m1(String param) {
        super.m1(param);
    }

    @Override
    public void m2() {
        super.m2();
    }

    @Override
    protected void m3() {
        super.m3();
    }

    public static void s2() {

    }

    @Override
    public void all() {
        s2();
        super.all();
    }
}
