package com.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

public class Complex1 {
    private long test1;
    private long[] test2;
    private Complex1Sub sub;
    private Complex1Sub[] subArray;
    private List<Complex1Sub> subList;
    private Map<Complex1Sub, Complex1Sub> doubleMap;
    private Map<String, Complex1Sub> singleMap;

    private Measure measure;

    @ConstructorProperties({"test1", "test2", "sub", "subArray", "subList", "doubleMap", "singleMap"})
    public Complex1(Measure measure, long test1, long[] test2, Complex1Sub sub, Complex1Sub[] subArray, List<Complex1Sub> subList, Map<Complex1Sub, Complex1Sub> doubleMap, Map<String, Complex1Sub> singleMap) {
        this.measure = measure;
        this.test1 = test1;
        this.test2 = test2;
        this.sub = sub;
        this.subArray = subArray;
        this.subList = subList;
        this.doubleMap = doubleMap;
        this.singleMap = singleMap;
    }

    public Measure getMeasure() {
        return measure;
    }

    public long getTest1() {
        return test1;
    }

    public long[] getTest2() {
        return test2;
    }

    public Complex1Sub getSub() {
        return sub;
    }

    public Complex1Sub[] getSubArray() {
        return subArray;
    }

    public List<Complex1Sub> getSubList() {
        return subList;
    }

    public Map<Complex1Sub, Complex1Sub> getDoubleMap() {
        return doubleMap;
    }

    public Map<String, Complex1Sub> getSingleMap() {
        return singleMap;
    }
}
