package dev.jvmguard.integration.tests.jvmguard.matched;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.ParameterClass;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Name1;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Sub1;

public class MatchedStaticNamingWorkload extends AbstractJvmGuardRun {

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
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Sub1"};
            case 3:
                return new String[0];
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
