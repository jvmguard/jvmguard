package dev.jvmguard.integration.tests.jvmguard.matched;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry1;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry1Sub;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry3;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry3Sub;

public class MatchedReentryWorkload extends AbstractJvmGuardRun {

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
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry3",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry3Sub"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
