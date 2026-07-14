package dev.jvmguard.agent.mbean;

import java.io.Serializable;

public interface MBeanModificationData extends Serializable {
    String getErrorMessage();
    String getStackTrace();
}
