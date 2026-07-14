package dev.jvmguard.integration.tests.jvmguard.declared;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNaming;
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNamingSub1;
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNamingSub1_1;
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNamingSub2;

public class DeclaredMethodNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<1; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<2; i++) {
            call(new DeclaredMethodNaming());
            call(new DeclaredMethodNamingSub1());
            call(new DeclaredMethodNamingSub1_1());
            call(new DeclaredMethodNamingSub2());
        }
    }

    private void call(DeclaredMethodNaming object) {
        object.outerNtest1(333, "param", new DeclaredMethodNaming.ParameterClass(999));
        object.ntest2(333, "param", new DeclaredMethodNaming.ParameterClass(999));
        object.ntest3();
        object.ntest4();
        object.ntest5();
        object.ntest6();
        object.ntest7();
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
