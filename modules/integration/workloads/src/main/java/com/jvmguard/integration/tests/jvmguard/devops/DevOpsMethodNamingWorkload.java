package com.jvmguard.integration.tests.jvmguard.devops;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.DevopsMethodNaming;
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.DevopsMethodNamingSub1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.DevopsMethodNamingSub1_1;
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.DevopsMethodNamingSub2;

public class DevOpsMethodNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<1; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<2; i++) {
            call(new DevopsMethodNaming());
            call(new DevopsMethodNamingSub1());
            call(new DevopsMethodNamingSub1_1());
            call(new DevopsMethodNamingSub2());
        }
    }

    private void call(DevopsMethodNaming object) {
        object.outerNtest1(333, "param", new DevopsMethodNaming.ParameterClass(999));
        object.ntest2(333, "param", new DevopsMethodNaming.ParameterClass(999));
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
