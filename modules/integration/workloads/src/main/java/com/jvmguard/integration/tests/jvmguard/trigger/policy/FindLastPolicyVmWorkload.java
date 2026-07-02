package com.jvmguard.integration.tests.jvmguard.trigger.policy;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.integration.AbstractJvmGuardRun;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FindLastPolicyVmWorkload extends AbstractJvmGuardRun {
    public static final Logger LOGGER = Logger.getAnonymousLogger();

    static {
        LOGGER.setUseParentHandlers(false);
    }

    @Override
    protected void work() throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 4; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        methodOne();
                        methodTwo();
                        methodThree1();
                        methodThree2();
                    }
                }
            });
        }
    }

    @MethodTransaction
    public void methodOne() {
        if (getVmNo() == 4) {
            slow();
        } else if (getVmNo() == 5) {
            verySlow();
        } else if (getVmNo() == 6) {
            overdue();
        } else if (getVmNo() == 7) {
            error();
        } else {
            normal();
        }
    }

    @MethodTransaction
    public void methodTwo() {
        if (getVmNo() == 7) {
            slow();
        } else if (getVmNo() == 6) {
            verySlow();
        } else if (getVmNo() == 5) {
            overdue();
        } else if (getVmNo() == 4) {
            error();
        } else {
            normal();
        }
    }

    @MethodTransaction
    public void methodThree1() {
        if (getVmNo() == 4) {
            slow();
        } else if (getVmNo() == 7) {
            error();
        } else {
            normal();
        }
    }

    @MethodTransaction
    public void methodThree2() {
        if (getVmNo() == 5) {
            verySlow();
        } else if (getVmNo() == 6) {
            overdue();
        } else {
            normal();
        }
    }

    private void normal() {
        sleep(50);
    }

    private void slow() {
        sleep(450);
    }

    private void verySlow() {
        sleep(650);
    }

    private void overdue() {
        sleep(2500);
    }

    private void error() {
        LOGGER.severe("error");
    }
}
