package com.jvmguard.integration.tests.jvmguard.pojo.classes.modifier;

public class SubModifier2 extends Modifier2 {
    @Override
    public void b1() {
        pp1();
        pub1();
    }

    @Override
    public void b2() {
        pro1();
        pub1();
    }

    @Override
    public void b3() {
        priv1();
        pub1();
    }

    @Override
    void pp1() {
    }

    @Override
    protected void pro1() {
    }

    private void priv1() {
    }

    @Override
    public void pub1() {
    }
}
