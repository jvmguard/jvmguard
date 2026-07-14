package dev.jvmguard.integration.tests.jvmguard.matched;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.ParameterClass;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Name1;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Sub1;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.SubName1;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.SubName2;

public class MatchedNamingWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<3; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<4; i++) {
            invoke(new SubName1());
            invoke(new SubName2());
            invoke(new Name1());
            new Sub1().b3();
        }
    }

    private void invoke(Name1 name1) {
        name1.outerInvoke(new ParameterClass(), "inv1", 1001);
        name1.outerInvoke2(new ParameterClass(), "inv2", 1002);
        name1.outerInvoke3(new ParameterClass(), "inv3", 1003);
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Sub1"};
            case 3:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Name1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.SubName2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.SubName1"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
