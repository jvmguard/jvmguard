package com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry;

public class Reentry1Sub extends Reentry1 {
    @Override
    public void m1(int i) {
        super.m1(i);
    }
}
