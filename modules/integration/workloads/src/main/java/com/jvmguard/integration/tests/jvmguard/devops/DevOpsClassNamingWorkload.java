package com.jvmguard.integration.tests.jvmguard.devops;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.*;

public class DevOpsClassNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<3; i++) {
            if (i==0) invokeFirst();
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeFirst() {
        new ClassNaming3().all();
        new ClassNaming4().all();
        new ClassNaming5().all();
        new ClassNaming6().all();
        new ClassNaming7().all();
        new ClassNaming8().all();
        new Sub1ClassNaming4().all();
        new Sub1ClassNaming7().all();
        new Sub1ClassNaming8().all();
        new Sub2ClassNaming8().all();

        new Sub1ClassNaming5().all();
        new Sub2ClassNaming5().all();
        new Sub3ClassNaming5().all();
    }

    private void invokeEverything() {
        for (int i=0; i<2; i++) {
            call(new Sub1ClassNaming2());
            call(new Sub2ClassNaming2());
            call(new Sub3ClassNaming2());
            call(new Sub4ClassNaming2());
        }
        call(new Sub1ClassNaming1());
        call(new Sub2ClassNaming1());
    }

    private void call(ClassNaming2 object) {
        object.m1("usedparam");
        object.m2();
    }

    private void call(ClassNaming1 object) {
        call2(object, 500);
        call2(object, 150);
    }

    private void call2(ClassNaming1 object, int time) {
        object.m1(time);
        object.m2(time);
        object.m3(time);
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.devops.classes.naming.ClassNaming3",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.naming.ClassNaming1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.naming.Sub2ClassNaming1",
                    "com.jvmguard.integration.tests.jvmguard.devops.classes.naming.Sub1ClassNaming1"};
            case 3:
                return new String[0];
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
