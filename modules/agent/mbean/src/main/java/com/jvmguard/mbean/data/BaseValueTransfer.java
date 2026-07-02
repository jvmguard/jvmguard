package com.jvmguard.mbean.data;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseValueTransfer {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_BOOLEAN = 2;
    public static final int TYPE_BYTE = 3;
    public static final int TYPE_CHAR = 4;
    public static final int TYPE_SHORT = 5;
    public static final int TYPE_INT = 6;
    public static final int TYPE_LONG = 7;
    public static final int TYPE_FLOAT = 8;
    public static final int TYPE_DOUBLE = 9;
    public static final int TYPE_STRING_SHORT = 10;
    public static final int TYPE_STRING_LONG = 11;

    public static boolean write(DataOutput out, Object value, boolean convertUnknown) throws IOException {
        if (value == null) {
            out.writeByte(TYPE_NULL);
        } else if (value instanceof Boolean) {
            out.writeByte(TYPE_BOOLEAN);
            out.writeBoolean((Boolean)value);
        } else if (value instanceof Byte) {
            out.writeByte(TYPE_BYTE);
            out.writeByte((Byte)value);
        } else if (value instanceof Character) {
            out.writeByte(TYPE_CHAR);
            out.writeChar((Character)value);
        } else if (value instanceof Short) {
            out.writeByte(TYPE_SHORT);
            out.writeShort((Short)value);
        } else if (value instanceof Integer) {
            out.writeByte(TYPE_INT);
            out.writeInt((Integer)value);
        } else if (value instanceof Long) {
            out.writeByte(TYPE_LONG);
            out.writeLong((Long)value);
        } else if (value instanceof Float) {
            out.writeByte(TYPE_FLOAT);
            out.writeFloat((Float)value);
        } else if (value instanceof Double) {
            out.writeByte(TYPE_DOUBLE);
            out.writeDouble((Double)value);
        } else if (value instanceof String) {
            writeString(out, (String)value);
        } else if (convertUnknown) {
            writeUnknown(out, value);
        } else {
            return false;
        }
        return true;
    }

    static void writeString(DataOutput out, String value) throws IOException {
        if (value.length() < 20000) {
            out.writeByte(TYPE_STRING_SHORT);
            out.writeUTF(value);
        } else {
            out.writeByte(TYPE_STRING_LONG);
            writeOptionalLongString(out, value);
        }
    }

    public static void writeOptionalLongString(DataOutput out, String string) throws IOException {
        if (string == null) {
            out.writeInt(-1);
        } else {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    public static void writeUnknown(DataOutput out, Object value) throws IOException {
        String string;
        try {
            string = value.toString();
        } catch (Throwable t) {
            string = "[ERROR: " + t + "]";
        }
        writeString(out, string);
    }

    public static Object read(DataInput input) throws IOException {
        int type = input.readByte();
        switch (type) {
            case TYPE_NULL:
                return null;
            case TYPE_BOOLEAN:
                return input.readBoolean();
            case TYPE_BYTE:
                return input.readByte();
            case TYPE_CHAR:
                return input.readChar();
            case TYPE_SHORT:
                return input.readShort();
            case TYPE_INT:
                return input.readInt();
            case TYPE_LONG:
                return input.readLong();
            case TYPE_FLOAT:
                return input.readFloat();
            case TYPE_DOUBLE:
                return input.readDouble();
            case TYPE_STRING_SHORT:
                return input.readUTF();
            case TYPE_STRING_LONG:
                return readOptionalLongString(input);
        }
        return null;
    }

    public static String readOptionalLongString(DataInput input) throws IOException {
        int byteLength = input.readInt();
        if (byteLength == -1) {
            return null;
        } else {
            byte[] bytes = new byte[byteLength];
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
