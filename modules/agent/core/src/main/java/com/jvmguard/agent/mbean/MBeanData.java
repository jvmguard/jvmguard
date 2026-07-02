package com.jvmguard.agent.mbean;

import javax.management.MBeanInfo;
import java.io.Serializable;
import java.util.List;

public interface MBeanData extends Serializable {
    MBeanInfo getBeanInfo();
    List<Object> getValues();
}
