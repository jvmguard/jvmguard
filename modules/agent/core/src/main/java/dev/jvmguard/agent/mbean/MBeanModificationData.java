package dev.jvmguard.agent.mbean;

import java.io.Serializable;

public interface MBeanModificationData extends Serializable {
    String getErrorMessage();
    String getStackTrace();

    default String getErrorKey() {
        return null;
    }
    default Object[] getErrorParams() {
        return new Object[0];
    }
}
