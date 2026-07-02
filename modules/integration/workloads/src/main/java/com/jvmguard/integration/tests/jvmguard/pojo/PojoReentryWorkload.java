package com.jvmguard.integration.tests.jvmguard.pojo;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry1;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry1Sub;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry3;
import com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry3Sub;

public class PojoReentryWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<4; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<4; i++) {
            new Reentry1().m1(5);
            new Reentry1Sub().m1(5);
            new Reentry3().m1();
            new Reentry3Sub().m1();
            Reentry3Sub.s1();
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
            case 3:
            case 4:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry3",
                    "com.jvmguard.integration.tests.jvmguard.pojo.classes.reentry.Reentry3Sub"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
