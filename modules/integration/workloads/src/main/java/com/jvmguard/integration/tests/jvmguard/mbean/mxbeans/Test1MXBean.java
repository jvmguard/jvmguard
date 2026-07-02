package com.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Test1MXBean {

    Complex1Sub add(int val1, int val2, String[] params, int[] params2);
    int[] add2(String[][] params1, int[][] params2);
    String add3(boolean p1, byte p2, short p3, char p4, int p5, float p6, long p7, double p8, String p9, BigDecimal p10, BigInteger p11, Date p12, String p13);

    Measure getMeasure();
    Test1MXBean getParent();
    void setParent(Test1MXBean parent);
    Complex1 getComplex();
    long[][] getPrimitiveArray2();
    Complex1Sub[] getComplexArray();
    List<Complex1Sub> getComplexList();
    Map<Integer,Complex1> getMap1();
    Map<Complex1Sub,String> getMap2();
    Map<Date,Long> getMap3();
    Map<String,Complex1Sub> getMap4();
    Map<Measure,Complex1> getMap5();
    Complex1 getComplexNull();
    Long getLongNull();

    String getShortString();
    String getLongString();
}
