package com.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import com.jvmguard.annotation.NoTransaction;

@NoTransaction
public class Sub2ClassNaming8 extends ClassNaming8 {
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

    @Override
    public void m4() {
        super.m4();
    }

    @Override
    public void all() {
        super.all();
    }
}
