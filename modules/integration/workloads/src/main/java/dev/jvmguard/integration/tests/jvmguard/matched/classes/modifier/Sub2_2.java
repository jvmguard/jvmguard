package dev.jvmguard.integration.tests.jvmguard.matched.classes.modifier;

public class Sub2_2 extends Sub2 {
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
