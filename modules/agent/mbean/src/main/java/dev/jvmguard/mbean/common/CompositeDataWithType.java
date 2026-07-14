package dev.jvmguard.mbean.common;

import javax.management.openmbean.CompositeType;
import java.util.Arrays;

public class CompositeDataWithType {
    private CompositeType compositeType;
    private Object[] values;

    public CompositeDataWithType(CompositeType compositeType, Object[] values) {
        this.compositeType = compositeType;
        this.values = values;
    }

    public CompositeType getCompositeType() {
        return compositeType;
    }

    public Object[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "CompositeDataWithType{" +
            "compositeType=" + compositeType +
            ", values=" + Arrays.toString(values) +
            '}';
    }
}
