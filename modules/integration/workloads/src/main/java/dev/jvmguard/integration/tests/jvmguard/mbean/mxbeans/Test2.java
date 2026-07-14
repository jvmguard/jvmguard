package dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class Test2 implements Test2MXBean {
    private boolean booleanVal = true;
    private byte byteVal = 3;
    private char charVal = 'A';
    private short shortVal = 3;
    private int intVal = 4;
    private float floatVal = 5.5f;
    private long longVal = 6;
    private double doubleVal = 7.7d;
    private String string = "test";
    private Date date = new Date(10000);
    private BigInteger bigInteger = new BigInteger("123");
    private BigDecimal bigDecimal = new BigDecimal("123.33");

    private Boolean booleanVal2 = true;
    private Byte byteVal2 = 3;
    private Character charVal2 = 'A';
    private Short shortVal2 = 3;
    private Integer intVal2 = 4;
    private Float floatVal2 = 5.5f;
    private Long longVal2 = 6L;
    private Double doubleVal2 = 7.7d;

    private int[] intArray = { 1, 2, 3 };
    private long[] longArray = { 1, 2, 3 };
    private double[] doubleArray = { 1.1d, 2.2d, 3.3d };
    private String[] stringArray = { "test1", null, "test2" };
    private BigDecimal[] bigDecimalArray = { new BigDecimal("1.1"), new BigDecimal("2.2") };
    private Long[] longArray2 = {1L, null, 3L};


    @Override
    public int[] getIntArray() {
        return intArray;
    }

    @Override
    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    @Override
    public long[] getLongArray() {
        return longArray;
    }

    @Override
    public void setLongArray(long[] longArray) {
        this.longArray = longArray;
    }

    @Override
    public double[] getDoubleArray() {
        return doubleArray;
    }

    @Override
    public void setDoubleArray(double[] doubleArray) {
        this.doubleArray = doubleArray;
    }

    @Override
    public String[] getStringArray() {
        return stringArray;
    }

    @Override
    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    @Override
    public BigDecimal[] getBigDecimalArray() {
        return bigDecimalArray;
    }

    @Override
    public void setBigDecimalArray(BigDecimal[] bigDecimalArray) {
        this.bigDecimalArray = bigDecimalArray;
    }

    @Override
    public Long[] getLongArray2() {
        return longArray2;
    }

    @Override
    public void setLongArray2(Long[] longArray2) {
        this.longArray2 = longArray2;
    }

    @Override
    public boolean isBooleanVal() {
        return booleanVal;
    }

    @Override
    public void setBooleanVal(boolean booleanVal) {
        this.booleanVal = booleanVal;
    }

    @Override
    public byte getByteVal() {
        return byteVal;
    }

    @Override
    public void setByteVal(byte byteVal) {
        this.byteVal = byteVal;
    }

    @Override
    public char getCharVal() {
        return charVal;
    }

    @Override
    public void setCharVal(char charVal) {
        this.charVal = charVal;
    }

    @Override
    public short getShortVal() {
        return shortVal;
    }

    @Override
    public void setShortVal(short shortVal) {
        this.shortVal = shortVal;
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    @Override
    public float getFloatVal() {
        return floatVal;
    }

    @Override
    public void setFloatVal(float floatVal) {
        this.floatVal = floatVal;
    }

    @Override
    public long getLongVal() {
        return longVal;
    }

    @Override
    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    @Override
    public double getDoubleVal() {
        return doubleVal;
    }

    @Override
    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    @Override
    public String getString() {
        return string;
    }

    @Override
    public void setString(String string) {
        this.string = string;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public BigInteger getBigInteger() {
        return bigInteger;
    }

    @Override
    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    @Override
    public BigDecimal getBigDecimal() {
        return bigDecimal;
    }

    @Override
    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    @Override
    public Boolean getBooleanVal2() {
        return booleanVal2;
    }

    @Override
    public void setBooleanVal2(Boolean booleanVal2) {
        this.booleanVal2 = booleanVal2;
    }

    @Override
    public Byte getByteVal2() {
        return byteVal2;
    }

    @Override
    public void setByteVal2(Byte byteVal2) {
        this.byteVal2 = byteVal2;
    }

    @Override
    public Character getCharVal2() {
        return charVal2;
    }

    @Override
    public void setCharVal2(Character charVal2) {
        this.charVal2 = charVal2;
    }

    @Override
    public Short getShortVal2() {
        return shortVal2;
    }

    @Override
    public void setShortVal2(Short shortVal2) {
        this.shortVal2 = shortVal2;
    }

    @Override
    public Integer getIntVal2() {
        return intVal2;
    }

    @Override
    public void setIntVal2(Integer intVal2) {
        this.intVal2 = intVal2;
    }

    @Override
    public Float getFloatVal2() {
        return floatVal2;
    }

    @Override
    public void setFloatVal2(Float floatVal2) {
        this.floatVal2 = floatVal2;
    }

    @Override
    public Long getLongVal2() {
        return longVal2;
    }

    @Override
    public void setLongVal2(Long longVal2) {
        this.longVal2 = longVal2;
    }

    @Override
    public Double getDoubleVal2() {
        return doubleVal2;
    }

    @Override
    public void setDoubleVal2(Double doubleVal2) {
        this.doubleVal2 = doubleVal2;
    }
}
