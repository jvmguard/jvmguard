package dev.jvmguard.integration;

import dev.jvmguard.annotation.Telemetry;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJvmGuardRun extends AbstractRun {
    private static volatile int requestedConfigurationNumber = 1;
    static volatile int currentConfigurationNumber = 0;

    @Telemetry("requestedConfigurationNumber")
    private static int requestedConfigurationNumber() {
        return requestedConfigurationNumber;
    }

    protected void updateNextConfigurationNumber() {
        sleep(10000);
        requestedConfigurationNumber = ++currentConfigurationNumber;
        sleep(120000);

    }
    protected void waitForNextConfiguration() {
        requestedConfigurationNumber = currentConfigurationNumber + 1;
        System.out.println("WAITING FOR CONFIG " + requestedConfigurationNumber);
        while (currentConfigurationNumber < requestedConfigurationNumber) {
            sleep(100);
        }
        System.out.println("RECEIVED CONFIG " + requestedConfigurationNumber);
    }

    protected void work() throws Exception {
    }

    protected String[] getExpectedRetransformClasses(int newConfigNumber) {
        return null;
    }

    protected boolean checkRetransform(int newConfigNumber, Collection<Class> classes) {
        String[] expected = getExpectedRetransformClasses(newConfigNumber);
        return expected == null || toNames(classes).equals(toSet(expected));
    }

    protected static Set<String> toSet(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    protected static Set<String> toNames(Collection<Class> classes) {
        Set<String> ret = new HashSet<>();
        for (Class aClass : classes) {
            ret.add(aClass.getName());
        }
        return ret;
    }

    @Override
    public void run() {
        if (Boolean.getBoolean("jvmguard.noListener")) {
            System.out.println("Starting without listener");
            doWork();
        } else {
            try {
                JvmGuardRunListener.initListener(this);

                System.out.println("SLEEPING");
                try {
                    Thread.sleep(1000L * 10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Throwable e) {
                //noinspection ThrowablePrintedToSystemOut
                System.out.println(e);
                System.out.println("STARTING WITHOUT AGENT");
                doWork();
            }
        }
    }

    private void doWork() {
        try {
            work();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    protected void assertEqual(int v1, int v2) {
        if (v1 != v2) throw new RuntimeException(v1 + " != " + v2);
    }

    protected void assertEqual(long v1, long v2) {
        if (v1 != v2) throw new RuntimeException(v1 + " != " + v2);
    }

    protected void assertTrue(boolean expr) {
        if (!expr) throw new RuntimeException();
    }

}
