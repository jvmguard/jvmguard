package dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming;

@MAnno1
public class Class2 {
    @CAnno1
    public void unused() {
    }

    @MAnno1
    public void manno1_1() {
        manno1_inner1();
        manno1_inner2();
    }

    @MAnno1
    public void manno1_2() {
        manno2();
        manno3();
    }

    @MAnno1
    private void manno1_inner1() {
    }

    @MAnno1
    public void manno1_inner2() {
    }

    @MAnno2
    public void manno2() {
    }

    @MAnno3
    public void manno3() {
    }

    @Override
    public String toString() {
        return "tostring: " + getClass().getName();
    }
}
