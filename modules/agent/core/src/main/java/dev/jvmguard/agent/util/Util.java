package dev.jvmguard.agent.util;

public class Util {
    public static final String OS_LOWER_CASE = System.getProperty("os.name", "").toLowerCase();
    private static final boolean WINDOWS = OS_LOWER_CASE.startsWith("win");
    private static final boolean MAC = OS_LOWER_CASE.startsWith("mac");
    private static final String JAVA_VERSION = System.getProperty("java.version", "");
    private static final boolean JAVA9_PLUS = !JAVA_VERSION.startsWith("1.");
    private static final boolean JAVA_1_8 = !JAVA9_PLUS && JAVA_VERSION.startsWith("1.8");

    public static final int JAVA_MAJOR_VERSION;

    private static final boolean J9VM;
    private static final boolean OPENJ9;

    static {
        String vmVendor = System.getProperty("java.vm.vendor", "").toLowerCase();
        String vendor = System.getProperty("java.vendor", "").toLowerCase();
        OPENJ9 = vmVendor.contains("openj9") || vendor.contains("openj9");
        J9VM = vmVendor.contains("ibm") || vendor.contains("ibm") || OPENJ9;

        String usedVersion = JAVA_VERSION;
        if (usedVersion.startsWith("1.")) {
            usedVersion = usedVersion.substring(2);
        }
        int dotIndex = usedVersion.indexOf('.');
        if (dotIndex > -1) {
            usedVersion = usedVersion.substring(0, dotIndex);
        }
        int dashIndex = usedVersion.indexOf('-');
        if (dashIndex > -1) {
            usedVersion = usedVersion.substring(0, dashIndex);
        }
        int javaMajorVersion;
        try {
            javaMajorVersion = Integer.parseInt(usedVersion);
        } catch (NumberFormatException e) {
            javaMajorVersion = 0;
        }
        JAVA_MAJOR_VERSION = javaMajorVersion;

    }


    public static String buildMessage(String message, Throwable t) {
        StringBuilder builder = new StringBuilder();
        if (message != null) {
            builder.append(message).append(": ");
        }
        builder.append(t.toString()).append("\n");
        StackTraceElement[] stackTraceElements = t.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            builder.append(stackTraceElement).append("\n");
        }
        if (builder.length() > 32000) {
            builder.setLength(32000);
        }
        return builder.toString();
    }

    public static boolean isJava9Plus() {
        return JAVA9_PLUS;
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isJ9VM() {
        return J9VM;
    }

    public static boolean isJava18Plus() {
        return JAVA_1_8 || JAVA9_PLUS;
    }

    public static boolean isMac() {
        return MAC;
    }

    public static long divideAndRoundUpToOne(long v1, long v2) {
        long ret = v1 / v2;
        if (ret == 0 && v1 > 0) {
            return 1;
        } else if (v2 > 1 && (v1 % v2) >= v2 / 2) {
            ret++;
        }
        return ret;
    }
}
