package dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class Test1 implements Test1MXBean {
    private Test1MXBean parent;
    private Complex1 complex1;

    public Test1(Test1MXBean parent, Complex1 complex1) {
        this.parent = parent;
        this.complex1 = complex1;
    }

    @Override
    public Test1MXBean getParent() {
        return parent;
    }

    @Override
    public void setParent(Test1MXBean parent) {
        this.parent = parent;
    }

    @Override
    public Complex1 getComplex() {
        return complex1;
    }

    @Override
    public Complex1Sub add(int val1, int val2, String[] params, int[] params2) {
        return new Complex1Sub(val1 + val2 + params2[0], params.length + " " + params[0]);
    }

    @Override
    public int[] add2(String[][] params1, int[][] params2) {
        return params2[0];
    }

    @Override
    public String add3(boolean p1, byte p2, short p3, char p4, int p5, float p6, long p7, double p8, String p9, BigDecimal p10, BigInteger p11, Date p12, String p13) {
        return "ret " + p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12 + p13;
    }

    @Override
    public Complex1 getComplexNull() {
        return null;
    }

    @Override
    public Long getLongNull() {
        return null;
    }

    @Override
    public long[][] getPrimitiveArray2() {
        return new long[][]{{1, 2, 3}, {3, 4, 5}};
    }

    @Override
    public Complex1Sub[] getComplexArray() {
        return new Complex1Sub[] { new Complex1Sub(1, "test"), new Complex1Sub(2, "test2")};
    }

    @Override
    public List<Complex1Sub> getComplexList() {
        return Arrays.asList(getComplexArray());
    }

    @Override
    public Map<Integer, Complex1> getMap1() {
        HashMap<Integer, Complex1> map = new HashMap<>();
        for (Complex1Sub complex1Sub : getComplexArray()) {
            map.put((int)complex1Sub.getSub1(), new Complex1(getMeasure(), complex1Sub.getSub1(), null, complex1Sub, null, null, null, getMap4()));
        }
        return map;
    }

    @Override
    public Map<Complex1Sub, String> getMap2() {
        HashMap<Complex1Sub, String> map = new HashMap<>();
        for (Complex1Sub complex1Sub : getComplexArray()) {
            map.put(complex1Sub, complex1Sub.getSub2());
        }
        return map;
    }

    @Override
    public Map<Date, Long> getMap3() {
        HashMap<Date, Long> map = new HashMap<>();
        map.put(new Date(1000000), 1000000L);
        map.put(new Date(2000000), 2000000L);
        return map;
    }

    @Override
    public Map<String, Complex1Sub> getMap4() {
        HashMap<String, Complex1Sub> map = new HashMap<>();
        for (Complex1Sub complex1Sub : getComplexArray()) {
            map.put(complex1Sub.getSub2(), complex1Sub);
        }
        return map;
    }

    @Override
    public Map<Measure, Complex1> getMap5() {
        return complex1 == null ? Collections.emptyMap() : Collections.singletonMap(complex1.getMeasure(), complex1);
    }

    @Override
    public Measure getMeasure() {
        return new Measure("ms", 3.3d);
    }


    @Override
    public String getShortString() {
        return "short";
    }

    @Override
    public String getLongString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 8000; i++) {
            ret.append("1234567890");
        }
        return ret.toString();
    }
}
