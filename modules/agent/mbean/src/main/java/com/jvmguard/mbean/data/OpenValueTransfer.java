package com.jvmguard.mbean.data;

import com.jvmguard.mbean.common.CompositeDataWithType;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class OpenValueTransfer {
    private static final int TYPE_ARRAY = 1;
    private static final int TYPE_BIG_DECIMAL = 12;
    private static final int TYPE_BIG_INTEGER = 13;
    private static final int TYPE_DATE = 14;
    private static final int TYPE_OBJECT_NAME = 15;
    private static final int TYPE_COMPOSITE_DATA_AS_ARRAY = 16;
    private static final int TYPE_TABULAR_DATA = 17;
    private static final int TYPE_COMPOSITE_DATA = 18;

    public static void write(DataOutput out, Object value, OpenType descriptorOpenType) throws IOException {
        if (BaseValueTransfer.write(out, value, false)) {
            return;
        }
        if (value.getClass().isArray()) {
            OpenType descriptorElementType = null;
            if (descriptorOpenType instanceof ArrayType) {
                descriptorElementType = ((ArrayType)descriptorOpenType).getElementOpenType();
            }
            out.writeByte(TYPE_ARRAY);
            int length = Array.getLength(value);
            out.writeInt(length);
            for (int i = 0; i < length; i++) {
                write(out, Array.get(value, i), descriptorElementType);
            }
        } else if (value instanceof BigInteger) {
            out.writeByte(TYPE_BIG_INTEGER);
            writeBigInteger(out, (BigInteger)value);
        } else if (value instanceof BigDecimal) {
            out.writeByte(TYPE_BIG_DECIMAL);
            BigDecimal bigDecimal = (BigDecimal)value;
            writeBigInteger(out, bigDecimal.unscaledValue());
            out.writeInt(bigDecimal.scale());
        } else if (value instanceof Date) {
            out.writeByte(TYPE_DATE);
            Date date = (Date)value;
            out.writeLong(date.getTime());
        } else if (value instanceof ObjectName) {
            out.writeByte(TYPE_OBJECT_NAME);
            MBeanTransfer.writeUTF(out, ((ObjectName)value).getCanonicalName());
        } else if (value instanceof CompositeData) {
            writeCompositeValues(out, (CompositeData)value, descriptorOpenType instanceof CompositeType ? (CompositeType)descriptorOpenType : null);
        } else if (value instanceof TabularData) {
            CompositeType descriptorRowType = null;
            if (descriptorOpenType instanceof TabularType) {
                descriptorRowType = ((TabularType)descriptorOpenType).getRowType();
            }
            out.writeByte(TYPE_TABULAR_DATA);
            TabularData tabularData = (TabularData)value;
            Collection<?> values = tabularData.values();
            out.writeInt(values.size());
            for (Object singleValue : values) {
                writeCompositeValues(out, (CompositeData)singleValue, descriptorRowType);
            }
        } else {
            BaseValueTransfer.writeUnknown(out, value);
        }
    }

    private static void writeCompositeValues(DataOutput out, CompositeData compositeData, CompositeType descriptorOpenType) throws IOException {
        Collection<?> values = compositeData.values();
        CompositeType realCompositeType = compositeData.getCompositeType();
        Set<String> descriptorKeys = descriptorOpenType != null ? descriptorOpenType.keySet() : null;
        if (realCompositeType != null && !realCompositeType.equals(descriptorOpenType)) {
            out.writeByte(TYPE_COMPOSITE_DATA);
            OpenTypeTransfer.writeOpenType(out, realCompositeType);
            descriptorKeys = null;
        } else {
            out.writeByte(TYPE_COMPOSITE_DATA_AS_ARRAY);
        }
        if (descriptorKeys != null && values.size() != descriptorKeys.size()) {
            descriptorKeys = null;
        }
        out.writeInt(values.size());
        Iterator<String> keyIterator = descriptorKeys != null ? descriptorKeys.iterator() : null;
        for (Object singleValue : values) {
            OpenType valueDescriptorType = null;
            if (keyIterator != null) {
                valueDescriptorType = descriptorOpenType.getType(keyIterator.next());
            }
            write(out, singleValue, valueDescriptorType);
        }
    }

    private static void writeBigInteger(DataOutput out, BigInteger bigInteger) throws IOException {
        byte[] byteArray = bigInteger.toByteArray();
        out.writeInt(byteArray.length);
        out.write(byteArray);
    }

    private static BigInteger readBigInteger(DataInput input) throws IOException {
        int length = input.readInt();
        byte[] data = new byte[length];
        input.readFully(data);
        return new BigInteger(data);
    }

    public static Object read(DataInput input) throws IOException {
        int type = input.readByte();
        switch (type) {
            case BaseValueTransfer.TYPE_NULL:
                return null;
            case TYPE_ARRAY:
                int arrayLength = input.readInt();
                Object[] objectArray = new Object[arrayLength];
                for (int i = 0; i < arrayLength; i++) {
                    objectArray[i] = read(input);
                }
                return objectArray;
            case BaseValueTransfer.TYPE_BOOLEAN:
                return input.readBoolean();
            case BaseValueTransfer.TYPE_BYTE:
                return input.readByte();
            case BaseValueTransfer.TYPE_CHAR:
                return input.readChar();
            case BaseValueTransfer.TYPE_SHORT:
                return input.readShort();
            case BaseValueTransfer.TYPE_INT:
                return input.readInt();
            case BaseValueTransfer.TYPE_LONG:
                return input.readLong();
            case BaseValueTransfer.TYPE_FLOAT:
                return input.readFloat();
            case BaseValueTransfer.TYPE_DOUBLE:
                return input.readDouble();
            case BaseValueTransfer.TYPE_STRING_SHORT:
                return input.readUTF();
            case BaseValueTransfer.TYPE_STRING_LONG:
                return BaseValueTransfer.readOptionalLongString(input);
            case TYPE_BIG_INTEGER:
                return readBigInteger(input);
            case TYPE_BIG_DECIMAL:
                BigInteger unscaledVal = readBigInteger(input);
                int scale = input.readInt();
                return new BigDecimal(unscaledVal, scale);
            case TYPE_DATE:
                return new Date(input.readLong());
            case TYPE_OBJECT_NAME:
                try {
                    return new ObjectName(input.readUTF());
                } catch (MalformedObjectNameException e) {
                    MBeanManager.getLogAdapter().error(e);
                }
                return null;
            case TYPE_COMPOSITE_DATA_AS_ARRAY:
                return readCompositeData(input);
            case TYPE_COMPOSITE_DATA:
                CompositeType openType = (CompositeType)OpenTypeTransfer.readOpenType(input);
                Object[] data = readCompositeData(input);
                return new CompositeDataWithType(openType, data);
            case TYPE_TABULAR_DATA:
                int rowCount = input.readInt();
                Object[] tabularData = new Object[rowCount];
                for (int i = 0; i < rowCount; i++) {
                    tabularData[i] = read(input);
                }
                return tabularData;
        }
        return null;
    }

    private static Object[] readCompositeData(DataInput input) throws IOException {
        int valueSize = input.readInt();
        Object[] values = new Object[valueSize];
        for (int i = 0; i < values.length; i++) {
            values[i] = read(input);
        }
        return values;
    }

}
