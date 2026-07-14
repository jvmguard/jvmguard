package dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.math.BigDecimal;

public interface Test3MXBean {
    int getVal();

    void setVal(int val);

    double getVal2();

    BigDecimal getVal3();

    Long getVal4();

    String getVal5();
}
