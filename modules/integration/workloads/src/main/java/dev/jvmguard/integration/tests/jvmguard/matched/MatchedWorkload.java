package dev.jvmguard.integration.tests.jvmguard.matched;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.ParameterClass;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.SimpleMatched;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.b.AbstractSubB;
import dev.jvmguard.integration.tests.jvmguard.matched.classes.c.AbstractSubC;

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
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.c.SubClass1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.Class1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.c.Class1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.SubClass1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.c.EmptyAbstractSub1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.EmptyAbstractSub1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.c.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.c.SubClass2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.c.AbstractSubC",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.Class1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.SubClass2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.SimpleMatched",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.EmptyAbstractSub1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.b.AbstractSubB"};
            case 4:
                return new String[]{"dev.jvmguard.integration.tests.jvmguard.matched.classes.a.Class1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.Class2",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.AbstractSubA",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.EmptyAbstractSub1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass1",
                    "dev.jvmguard.integration.tests.jvmguard.matched.classes.a.SubClass2"};
        }
        throw new IllegalArgumentException("unknown retransform number " + newConfigNumber);
    }
}
