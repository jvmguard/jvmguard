package dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming;


import dev.jvmguard.integration.AbstractRun;

@CAnno1
public class Class1 {

    public void m1(int time) {
        AbstractRun.sleep(time);
    }

    public void m2(int time) {
        innerM2(time);
    }

    public void innerM2(int time) {
        AbstractRun.sleep(time);
    }

    public void m3(int time) {
        innerM3(time);
    }

    protected void innerM3(int time) {
        AbstractRun.sleep(time);
    }

    public static void s1(int time) {
        AbstractRun.sleep(time);
    }

    @Override
    public String toString() {
        return "tostring: " + getClass().getName();
    }
}
