package com.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.math.BigDecimal;

public class Test3 implements Test3MXBean {
    private int val;

    public Test3(int val) {
        this.val = val;
    }

    @Override
    public int getVal() {
        return val;
    }

    @Override
    public void setVal(int val) {
        this.val = val;
    }

    @Override
    public double getVal2() {
        return 56.78d;
    }

    @Override
    public BigDecimal getVal3() {
        return new BigDecimal("1300.33");
    }

    @Override
    public Long getVal4() {
        return null;
    }

    @Override
    public String getVal5() {
        return "test";
    }
}
