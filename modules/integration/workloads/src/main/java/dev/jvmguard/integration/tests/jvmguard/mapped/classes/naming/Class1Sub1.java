package dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming;


import dev.jvmguard.annotation.NoTransaction;
import dev.jvmguard.integration.AbstractRun;

public class Class1Sub1 extends Class1 {
    @Override
    public void m1(int time) {
        super.m1(time);
    }

    @Override
    public void m2(int time) {
        innerM2(time);
    }

    @Override
    public void m3(int time) {
        AbstractRun.sleep(time);
    }

    public void sub1(int time) {
        AbstractRun.sleep(time);
    }

    @NoTransaction
    public void sub2(int time) {
        AbstractRun.sleep(time);
    }

    public static void ssub1(int time) {
        AbstractRun.sleep(time);
    }
}
