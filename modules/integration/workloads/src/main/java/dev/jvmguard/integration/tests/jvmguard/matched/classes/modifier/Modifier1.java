package dev.jvmguard.integration.tests.jvmguard.matched.classes.modifier;

public class Modifier1 {
    public void b1() {
        pp1();
        pub1();
    }

    public void b2() {
        pro1();
        pub1();
    }

    public void b3() {
        priv1();
        pub1();
    }

    void pp1() {
    }

    protected void pro1() {
    }

    private void priv1() {
    }

    public void pub1() {
    }
}
