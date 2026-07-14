package dev.jvmguard.agent.util;

import java.io.Serializable;

public class MutableInt implements Serializable {
    public int val;

    public MutableInt() {
    }

    @Override
    public String toString() {
        return String.valueOf(val);
    }
}
