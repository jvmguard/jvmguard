package com.jvmguard.agent.mbean;

public interface MBeanOperationData extends MBeanModificationData {
    Object getReturnValue();
}
