package dev.jvmguard.integration.tests.jvmguard.mapped;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class1Sub1;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2;
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2Sub1;

public class MappedStaticNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<4; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        new Class1Sub1().m1(100);
        new Class2Sub1().manno3();
        new Class2().manno3();
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.Class2Sub1"};
            case 3:
                return new String[0];
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
