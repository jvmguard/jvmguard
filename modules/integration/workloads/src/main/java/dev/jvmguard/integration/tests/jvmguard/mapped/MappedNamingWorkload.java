package dev.jvmguard.integration.tests.jvmguard.mapped;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class1;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class1Sub1;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2Sub1;

public class MappedNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<4; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        callCommon(new Class1(), 700);
        callCommon(new Class1(), 100);
        Class1.s1(100);

        callCommon(new Class1Sub1(), 700);
        callCommon(new Class1Sub1(), 100);
        Class1Sub1.ssub1(100);
        new Class1Sub1().sub1(700);
        new Class1Sub1().sub1(100);
        new Class1Sub1().sub2(100);

        call(new Class2());
        call(new Class2Sub1());
    }

    private void call(Class2 class2) {
        class2.manno1_1();
        class2.manno1_2();
        class2.manno2();
        class2.manno3();
        class2.unused();
    }

    private void callCommon(Class1 class1, int time) {
        class1.m1(time);
        class1.m2(time);
        class1.m3(time);
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
            case 3:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class1",
                    "dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class1Sub1"};
            case 4:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2Sub1"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
