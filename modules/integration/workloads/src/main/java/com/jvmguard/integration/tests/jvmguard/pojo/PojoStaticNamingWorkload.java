package com.jvmguard.integration.tests.jvmguard.pojo;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.ParameterClass;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.naming.Name1;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.naming.Sub1;

public class PojoStaticNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<3; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<4; i++) {
            new Name1().outerInvoke(new ParameterClass(), "inv1", 100);
            new Sub1().b3();
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.pojo.classes.naming.Sub1"};
            case 3:
                return new String[0];
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
