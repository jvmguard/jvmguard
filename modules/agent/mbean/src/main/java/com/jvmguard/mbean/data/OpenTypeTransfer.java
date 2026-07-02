package com.jvmguard.mbean.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.management.Descriptor;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class OpenTypeTransfer {

    private static final String OPEN_TYPE_FIELD_NAME = "openType";
    private static final String[] FIELD_NAMES = {OPEN_TYPE_FIELD_NAME};

    public static void writeOpenTypeDescriptor(DataOutput out, Descriptor descriptor) throws IOException {
        OpenType openType = getOpenType(descriptor);
        if (openType != null) {
            writeOpenType(out, openType);
        } else {
            out.writeByte(Type.NONE.getIntValue());
        }
    }

    public static OpenType getOpenType(Descriptor descriptor) throws IOException {
        if (descriptor != null) {
            Object openType = descriptor.getFieldValue(OPEN_TYPE_FIELD_NAME);
            if (openType instanceof OpenType) {
                return (OpenType)openType;
            }
        }
        return null;
    }

    public static Descriptor readOpenTypeDescriptor(DataInput in) throws IOException {
        OpenType openType = readOpenType(in);
        return openType == null ? null : new DescriptorSupport(FIELD_NAMES, new Object[] {openType});
    }

    public static void writeOpenType(DataOutput out, OpenType openType) throws IOException {
        if (openType instanceof SimpleType) {
            out.writeByte(Type.fromSimpleType(openType).getIntValue());
        } else if (openType instanceof ArrayType) {
            out.writeByte(Type.ARRAY.getIntValue());
            ArrayType arrayType = (ArrayType)openType;
            out.writeInt(arrayType.getDimension());
            out.writeBoolean(arrayType.isPrimitiveArray());
            writeOpenType(out, arrayType.getElementOpenType());
        } else if (openType instanceof CompositeType) {
            out.writeByte(Type.COMPOSITE.getIntValue());
            writeComplexCommon(out, openType);

            CompositeType compositeType = (CompositeType)openType;
            Set<String> names = compositeType.keySet();
            out.writeInt(names.size());
            for (String name : names) {
                MBeanTransfer.writeUTF(out, name);
                MBeanTransfer.writeUTF(out, compositeType.getDescription(name));
                writeOpenType(out, compositeType.getType(name));
            }
        } else if (openType instanceof TabularType) {
            out.writeByte(Type.TABULAR.getIntValue());
            writeComplexCommon(out, openType);

            TabularType tabularType = (TabularType)openType;
            writeOpenType(out, tabularType.getRowType());
            List<String> indexNames = tabularType.getIndexNames();
            out.writeInt(indexNames.size());
            for (String indexName : indexNames) {
                MBeanTransfer.writeUTF(out, indexName);
            }
        } else {
            out.writeByte(Type.NONE.getIntValue());
        }
    }

    private static void writeComplexCommon(DataOutput out, OpenType openType) throws IOException {
        MBeanTransfer.writeUTF(out, openType.getTypeName());
        MBeanTransfer.writeUTF(out, openType.getDescription());
    }

    @SuppressWarnings("unchecked")
    public static OpenType readOpenType(DataInput in) throws IOException {
        try {
            Type type = Type.fromIntValue(in.readByte());
            if (type == Type.NONE) {
                return null;
            } else if (type == Type.ARRAY) {
                int dimensions = in.readInt();
                boolean primitive = in.readBoolean();
                OpenType elementType = readOpenType(in);
                if (elementType != null) {
                    if (primitive) {
                        ArrayType arrayType = new ArrayType((SimpleType)elementType, true);
                        if (dimensions <= 1) {
                            return arrayType;
                        } else {
                            return new ArrayType(dimensions - 1, arrayType);
                        }
                    } else {
                        return new ArrayType(dimensions, elementType);
                    }
                }
            } else if (type.getSimpleType() != null) {
                return type.getSimpleType();
            } else {
                String typeName = in.readUTF();
                String description = in.readUTF();
                switch (type) {
                    case COMPOSITE:
                        int count = in.readInt();
                        String[] itemNames = new String[count];
                        String[] itemDescriptions = new String[count];
                        OpenType[] itemTypes = new OpenType[count];
                        for (int i = 0; i < count; i++) {
                            itemNames[i] = in.readUTF();
                            itemDescriptions[i] = in.readUTF();
                            itemTypes[i] = readOpenType(in);
                        }
                        return new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
                    case TABULAR:
                        CompositeType rowType = (CompositeType)readOpenType(in);
                        int indexLength = in.readInt();
                        String[] indexNames = new String[indexLength];
                        for (int i = 0; i < indexLength; i++) {
                            indexNames[i] = in.readUTF();
                        }
                        //noinspection ConstantConditions
                        return new TabularType(typeName, description, rowType, indexNames);
                }
            }
            throw new RuntimeException("unknown open type " + type + ", " + type.getIntValue());
        } catch (OpenDataException e) {
            throw new IOException(e);
        }
    }

    private enum Type {
        NONE(0),
        VOID(1, SimpleType.VOID),
        BOOLEAN(2, SimpleType.BOOLEAN),
        CHARACTER(3, SimpleType.CHARACTER),
        BYTE(4, SimpleType.BYTE),
        SHORT(5, SimpleType.SHORT),
        INTEGER(6, SimpleType.INTEGER),
        LONG(7, SimpleType.LONG),
        FLOAT(8, SimpleType.FLOAT),
        DOUBLE(9, SimpleType.DOUBLE),
        STRING(10, SimpleType.STRING),
        @SuppressWarnings("SpellCheckingInspection")
        BIGDECIMAL(11, SimpleType.BIGDECIMAL),
        BIGINTEGER(12, SimpleType.BIGINTEGER),
        DATE(13, SimpleType.DATE),
        @SuppressWarnings("SpellCheckingInspection")
        OBJECTNAME(14, SimpleType.OBJECTNAME),

        ARRAY(15),
        COMPOSITE(16),
        TABULAR(17);

        private final int intValue;
        private final OpenType simpleType;

        private static final HashMap<OpenType, Type> simpleTypeToType = new HashMap<>();
        private static final Int2ObjectMap<Type> intValueToType = new Int2ObjectOpenHashMap<>();

        static {
            for (Type type : Type.values()) {
                intValueToType.put(type.getIntValue(), type);
                if (type.getSimpleType() != null) {
                    simpleTypeToType.put(type.getSimpleType(), type);
                }
            }
        }

        public static Type fromSimpleType(OpenType type) {
            Type ret = simpleTypeToType.get(type);
            return ret == null ? NONE : ret;
        }

        public static Type fromIntValue(int val) {
            Type ret = intValueToType.get(val);
            if (ret == null) {
                throw new IllegalArgumentException("unknown open type int value " + val);
            }
            return ret;
        }

        Type(int intValue, OpenType simpleType) {
            this.intValue = intValue;
            this.simpleType = simpleType;
        }

        Type(int intValue) {
            this.intValue = intValue;
            simpleType = null;
        }

        public int getIntValue() {
            return intValue;
        }

        public OpenType getSimpleType() {
            return simpleType;
        }
    }
}
