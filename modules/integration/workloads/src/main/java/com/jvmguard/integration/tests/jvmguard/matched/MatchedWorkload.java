package com.jvmguard.integration.tests.jvmguard.matched;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.matched.classes.ParameterClass;
import com.jvmguard.integration.tests.jvmguard.matched.classes.SimpleMatched;
import com.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA;
import com.jvmguard.integration.tests.jvmguard.matched.classes.b.AbstractSubB;
import com.jvmguard.integration.tests.jvmguard.matched.classes.c.AbstractSubC;

public class MatchedWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        for (int i=0; i<4; i++) {
            invokeEverything();
            waitForNextConfiguration();
        }
    }

    private void invokeEverything() {
        for (int i=0; i<30; i++) {
            new SimpleMatched().invoke2(new ParameterClass(), "invoke instance", 55);
            SimpleMatched.invoke(new ParameterClass(), "invoke static", 44);
            HierarchyCaller.call(AbstractSubA.class);
            HierarchyCaller.call(AbstractSubB.class);
            HierarchyCaller.call(AbstractSubC.class);
        }
    }

    @Override
    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        switch (newConfigNumber) {
            case 2:
            case 3:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.matched.classes.c.SubClass1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.Class1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.c.Class1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.SubClass1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.c.EmptyAbstractSub1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.EmptyAbstractSub1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.c.Class2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.Class2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.c.SubClass2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.c.AbstractSubC",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.Class1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.SubClass2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.SimpleMatched",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.EmptyAbstractSub1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.Class2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.b.AbstractSubB"};
            case 4:
                return new String[]{"com.jvmguard.integration.tests.jvmguard.matched.classes.a.Class1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.Class2",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.EmptyAbstractSub1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass1",
                    "com.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass2"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
