package com.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class Complex1Sub implements Serializable {
    private long sub1;
    private String sub2;

    @ConstructorProperties({"sub1", "sub2"})
    public Complex1Sub(long sub1, String sub2) {
        this.sub1 = sub1;
        this.sub2 = sub2;
    }

    public long getSub1() {
        return sub1;
    }

    public String getSub2() {
        return sub2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Complex1Sub that = (Complex1Sub)o;

        if (sub1 != that.sub1) {
            return false;
        }
        if (sub2 != null ? !sub2.equals(that.sub2) : that.sub2 != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)(sub1 ^ (sub1 >>> 32));
        result = 31 * result + (sub2 != null ? sub2.hashCode() : 0);
        return result;
    }
}
