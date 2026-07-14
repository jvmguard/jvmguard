package dev.jvmguard.mbean.common;

import javax.management.openmbean.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MBeanHelper {

    static Object convertParam(String paramString, String type) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (paramString == null) {
            return null;
        } else if (type.startsWith("[")) {
            if (type.startsWith("[[")) {
                throw new IllegalArgumentException("only one-dimensional arrays supported");
            }
            String[] singleStrings = paramString.isEmpty() ? new String[0] : paramString.split(",");
            return createArray(type, singleStrings);
        } else if (type.equals("java.lang.String")) {
            return paramString;
        } else if (type.equals("byte") || type.equals("java.lang.Byte")) {
            return Byte.parseByte(paramString);
        } else if (type.equals("char") || type.equals("java.lang.Character")) {
            return paramString.isEmpty() ? 0 : paramString.charAt(0);
        } else if (type.equals("double") || type.equals("java.lang.Double")) {
            return Double.parseDouble(paramString);
        } else if (type.equals("float") || type.equals("java.lang.Float")) {
            return Float.parseFloat(paramString);
        } else if (type.equals("int") || type.equals("java.lang.Integer")) {
            return Integer.parseInt(paramString);
        } else if (type.equals("long") || type.equals("java.lang.Long")) {
            return Long.parseLong(paramString);
        } else if (type.equals("short") || type.equals("java.lang.Short")) {
            return Short.parseShort(paramString);
        } else if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
            return Boolean.parseBoolean(paramString);
        } else if (type.equals("java.util.Date")) {
            return new Date(Long.parseLong(paramString));
        } else {
            Class<?> elementClass = Class.forName(type);
            Constructor constructor = elementClass.getConstructor(String.class);
            return constructor.newInstance(paramString);
        }
    }

    private static Object createArray(String type, String[] singleStrings) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        switch (type.charAt(1)) {
            case 'B':
                byte[] byteArray = new byte[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    byteArray[i] = Byte.parseByte(singleStrings[i]);
                }
                return byteArray;
            case 'C':
                char[] charArray = new char[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    charArray[i] = singleStrings[i].isEmpty() ? 0 : singleStrings[i].charAt(0);
                }
                return charArray;
            case 'D':
                double[] doubleArray = new double[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    doubleArray[i] = Double.parseDouble(singleStrings[i]);
                }
                return doubleArray;
            case 'F':
                float[] floatArray = new float[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    floatArray[i] = Float.parseFloat(singleStrings[i]);
                }
                return floatArray;
            case 'I':
                int[] intArray = new int[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    intArray[i] = Integer.parseInt(singleStrings[i]);
                }
                return intArray;
            case 'J':
                long[] longArray = new long[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    longArray[i] = Long.parseLong(singleStrings[i]);
                }
                return longArray;
            case 'S':
                short[] shortArray = new short[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    shortArray[i] = Short.parseShort(singleStrings[i]);
                }
                return shortArray;
            case 'Z':
                boolean[] booleanArray = new boolean[singleStrings.length];
                for (int i = 0; i < singleStrings.length; i++) {
                    booleanArray[i] = Boolean.parseBoolean(singleStrings[i]);
                }
                return booleanArray;
            case 'L':
                String className = type.substring(2, type.length() - 1);
                if (className.equals("java.lang.String")) {
                    return singleStrings;
                } else if (className.equals("java.lang.Character")) {
                    Character[] ret = new Character[singleStrings.length];
                    for (int i = 0; i < singleStrings.length; i++) {
                        ret[i] = singleStrings[i].isEmpty() ? null : singleStrings[i].charAt(0);
                    }
                    return ret;
                } else {
                    Class<?> elementClass = Class.forName(className);
                    Constructor constructor = elementClass.getConstructor(String.class);
                    Object[] ret = (Object[])Array.newInstance(elementClass, singleStrings.length);
                    for (int i = 0; i < singleStrings.length; i++) {
                        ret[i] = singleStrings[i].isEmpty() ? null : constructor.newInstance(singleStrings[i]);
                    }
                    return ret;
                }
        }
        return null;
    }

    public static Object findAttribute(Object base, String name) throws FindAttributeException {
        if (base == null) {
            throw new FindAttributeException("base is null for " + name);
        }
        if (name == null || name.isEmpty()) {
            throw new FindAttributeException("name is null for " + base);
        }
        Class clazz = base.getClass();
        if (clazz.isArray()) {
            try {
                int index = Integer.parseInt(name);
                int length = Array.getLength(base);
                if (length <= index) {
                    throw new FindAttributeException("array too short " + length + " <= " + index);
                }
                return Array.get(base, index);
            } catch (NumberFormatException e) {
                throw new FindAttributeException("expected an array index instead of " + name);
            }
        } else if (base instanceof TabularData) {
            TabularData tabularData = (TabularData)base;
            TabularType tabularType = tabularData.getTabularType();
            List<String> indexNames = tabularType.getIndexNames();
            if (indexNames.size() != 1) {
                throw new FindAttributeException("cannot use tabular index for attribute " + name + ", only single column index supported: " + indexNames.size());
            }
            OpenType openType = tabularType.getRowType().getType(indexNames.get(0));
            if (openType == null) {
                throw new FindAttributeException("cannot find index type for " + name + ", " + indexNames.get(0));
            }
            Object key;
            try {
                key = convertParam(name, openType.getClassName());
            } catch (Throwable e) {
                throw new FindAttributeException("cannot convert param " + name + " for type  " + openType.getClassName());
            }
            if (key == null) {
                throw new FindAttributeException("cannot convert param " + name + " for type  " + openType.getClassName());
            }
            try {
                return tabularData.get(new Object[] {key});
            } catch (InvalidKeyException e) {
                throw new FindAttributeException(e.toString());
            }
        } else if (base instanceof CompositeData) {
            CompositeData compositeData = (CompositeData)base;
            try {
                return compositeData.get(name);
            } catch (InvalidKeyException e) {
                throw new FindAttributeException(e.toString());
            }
        } else {
            throw new FindAttributeException("cannot retrieve attribute " + name + " from " + base.getClass());
        }
    }

    public static Object convertArray(Object objectArrayInput, String type) throws ClassNotFoundException {
        if (objectArrayInput != null && objectArrayInput.getClass().isArray() && type != null) {
            Object[] objectArray = (Object[])objectArrayInput;
            Class componentType = getComponentType(type);
            Object ret = Array.newInstance(componentType, objectArray.length);
            for (int i = 0; i < objectArray.length; i++) {
                Array.set(ret, i, convertArray(objectArray[i], componentType.getName()));
            }
            return ret;
        }
        return objectArrayInput;
    }

    private static Class getComponentType(String type) throws ClassNotFoundException {
        switch (type.charAt(1)) {
            case '[':
                return Class.forName(type.substring(1));
            case 'L':
                return Class.forName(type.substring(2, type.length() - 1));
            case 'B':
                return byte.class;
            case 'C':
                return char.class;
            case 'D':
                return double.class;
            case 'F':
                return float.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'S':
                return short.class;
            case 'Z':
                return boolean.class;
        }
        throw new IllegalArgumentException(type);
    }

    public static boolean isSimpleKeyMap(OpenType openType) {
        if (openType instanceof TabularType) {
            TabularType tabularType = (TabularType)openType;
            if (tabularType.getRowType().keySet().size() == 2 && tabularType.getIndexNames().size() == 1) {
                return tabularType.getRowType().getType(tabularType.getIndexNames().get(0)) instanceof SimpleType;
            }
        }
        return false;
    }

    public static String[] splitEscaped(String attributePath) {
        boolean escaped = false;
        List<String> ret = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < attributePath.length(); i++) {
            char c = attributePath.charAt((i));
            if (c == '/') {
                if (escaped) {
                    current.append(c);
                    escaped = false;
                } else {
                    ret.add(current.toString());
                    current.setLength(0);
                }
            } else if (c == '\\') {
                if (escaped) {
                    escaped = false;
                    current.append(c);
                } else {
                    escaped = true;
                }
            } else {
                checkWrongEscape(escaped, current);
                current.append(c);
                escaped = false;
            }
        }
        checkWrongEscape(escaped, current);
        if (current.length() > 0) {
            ret.add(current.toString());
        }
        return ret.toArray(new String[0]);
    }

    private static void checkWrongEscape(boolean escaped, StringBuilder current) {
        if (escaped) { // this is actually a wrong escape sequence of a single \ not followed by \ or /. Will be treated as a backslash instead of ignored or failure.
            current.append('\\');
        }
    }

    public static class FindAttributeException extends Exception {
        public FindAttributeException(String message) {
            super(message);
        }
    }

}
