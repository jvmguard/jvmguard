package dev.jvmguard.agent.util;

public class ClassNameFormatter {

    public enum PackageMode {
        NONE,
        ABBREVIATED,
        FULL
    }

    public static String apply(String className, PackageMode packageMode) {
        StringBuilder builder = new StringBuilder(className.length());
        append(builder, className, packageMode);
        return builder.toString();
    }

    public static void append(StringBuilder builder, String className, PackageMode packageMode) {
        if (packageMode == PackageMode.FULL) {
            builder.append(className);
            return;
        }
        int dotIndex = className.lastIndexOf('.');
        if (dotIndex < 0) {
            builder.append(className);
            return;
        }
        switch (packageMode) {
            case NONE:
                builder.append(className, dotIndex + 1, className.length());
                break;
            case ABBREVIATED:
                boolean appendNext = true;
                for (int i = 0; i < dotIndex; i++) {
                    char c = className.charAt(i);
                    if (c == '.') {
                        builder.append('.');
                        appendNext = true;
                    } else if (appendNext) {
                        builder.append(c);
                        appendNext = false;
                    }
                }
                builder.append(className, dotIndex, className.length());
                break;
        }
    }
}
