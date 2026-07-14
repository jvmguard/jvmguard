package dev.jvmguard.agent.mbean;

public interface MBeanOperationData extends MBeanModificationData {
    Object getReturnValue();
}
