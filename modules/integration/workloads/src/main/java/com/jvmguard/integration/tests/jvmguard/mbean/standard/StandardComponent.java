package com.jvmguard.integration.tests.jvmguard.mbean.standard;

import java.io.Serializable;

public class StandardComponent implements Serializable {
    private long val = 10;

    public long getVal() {
        return val;
    }

    public void setVal(long val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "StandardComponent toString";
    }
}
