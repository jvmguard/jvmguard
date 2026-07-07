package com.jvmguard.integration.tests.jvmguard.matched.classes.reentry;

public class Reentry1 {
    public void m1(int i) {
        if (i == 0) {
            Reentry2.m2();
        } else {
            m1(i-1);
        }
    }
}
