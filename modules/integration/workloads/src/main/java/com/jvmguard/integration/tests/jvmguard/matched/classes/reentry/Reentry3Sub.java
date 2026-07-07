package com.jvmguard.integration.tests.jvmguard.matched.classes.reentry;

public class Reentry3Sub extends Reentry3 {
    @Override
    public void m1() {
        sub1();
        super.m1();
    }

    public static void s1() {
        new Reentry3Sub().m1();
    }

    public void sub1() {
    }
}
