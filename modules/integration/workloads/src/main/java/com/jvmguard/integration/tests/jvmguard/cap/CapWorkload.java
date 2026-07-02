package com.jvmguard.integration.tests.jvmguard.cap;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Part.Type;
import com.jvmguard.integration.AbstractJvmGuardRun;

public class CapWorkload extends AbstractJvmGuardRun {

    @Override
    protected void work() {
        if (getRunNo() == 1) {
            for (int i = 0; i < 30; i++) {
                testMethod(i);
            }
            updateNextConfigurationNumber();
            for (int i = 30; i < 35; i++) {
                testMethod(i);
            }
            waitForNextConfiguration();
            for (int i = 35; i < 40; i++) {
                testMethod(i);
            }
            updateNextConfigurationNumber();
            for (int i = 40; i < 45; i++) {
                testMethod(i);
            }
            waitForNextConfiguration();
        } else {
            for (int i = 45; i < 70; i++) {
                testMethod(i);
            }
            updateNextConfigurationNumber();
            for (int i = 70; i < 75; i++) {
                testMethod(i);
            }
            for (int i = 0; i < 5; i++) {
                testMethod(i);
            }
            waitForNextConfiguration();
        }
    }

    @MethodTransaction(naming = { @Part(value = Type.TEXT, text = "transaction "), @Part(Type.PARAMETER) })
    private void testMethod(int id) {
    }
}
