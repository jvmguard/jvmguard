package com.jvmguard.integration.tests.jvmguard.matched;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.matched.classes.modifier.*;

public class MatchedModifierWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        invokeEverything();
        waitForNextConfiguration();
    }

    private void invokeEverything() {
        for (int i=0; i<5; i++) {
            invokeBase(new Sub2());
            invokeBase(new Sub2_2());
            Modifier1 modifier1 = new Modifier1();
            modifier1.b1();
            modifier1.b2();
            modifier1.b3();
            invokeModifier2(new Modifier2());
            invokeModifier2(new SubModifier2());
        }
    }

    private void invokeModifier2(Modifier2 modifier2) {
        modifier2.b1();
        modifier2.b2();
        modifier2.b3();
    }

    private void invokeBase(Sub2 sub2) {
        sub2.b1();
        sub2.b2();
        sub2.b3();
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
