package com.jvmguard.integration;

import static com.jvmguard.integration.Util.TEST_CLASS_PROP_NAME;

public class RunTestAction {
    /*




    */
    public static void main(String[] args) throws Throwable {     // must be line 18
        try { AbstractRun run = ((AbstractRun)Class.forName(System.getProperty(TEST_CLASS_PROP_NAME)).newInstance()); if (run.isReloading()) run.runReloaded(); System.gc(); run.run(); run.checkReloaded();
        } catch (AbstractRun.DoNotRunException e) {
            System.err.println("SKIPPING RUN");
        }
    }
}
