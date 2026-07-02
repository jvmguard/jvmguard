package com.jvmguard.integration.tests.jvmguard.mbean.standard;

import javax.management.openmbean.CompositeData;

@SuppressWarnings("unused")
public interface TestStandardMBean {
    int add(int x, StandardComponent y);
    int add(int x, int y);

    String getString();
    StandardComponent getStandard();
    int getInt();
    void setInt(int intValue);

    CompositeData getExplicitComposite();
}
