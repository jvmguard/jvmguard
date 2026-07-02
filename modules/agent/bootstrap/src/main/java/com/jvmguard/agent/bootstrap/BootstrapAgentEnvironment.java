package com.jvmguard.agent.bootstrap;

import java.io.File;
import java.net.URL;

public class BootstrapAgentEnvironment {

    public static final boolean DEBUG_BOOTSTRAP = Boolean.getBoolean("jvmguard.debugBootstrap");

    private static final String URL_FILE_PREFIX = "file:";
    private static final String URL_JAR_PREFIX = "jar:file:";

    private static final String PLATFORM_DESCRIPTOR_WINDOWS = "windows";
    private static final String PLATFORM_DESCRIPTOR_WINDOWS_X64 = "windows-x64";
    private static final String PLATFORM_DESCRIPTOR_LINUX_X86 = "linux-x86";
    private static final String PLATFORM_DESCRIPTOR_LINUX_X64 = "linux-x64";
    private static final String PLATFORM_DESCRIPTOR_MACOS = "macos";

    private static final String OS_NAME = System.getProperty("os.name");

    static {
        initLoadingInformation();
    }

    private static File baseDir;

    public static File getAgentBaseDir() {
        return baseDir;
    }

    private static void initLoadingInformation() {

        String className = BootstrapAgentEnvironment.class.getName().replace('.', '/') + ".class";
        String simpleName = className.substring(className.lastIndexOf('/') + 1);
        URL classUrl = getResourceUrl(simpleName);
        if (classUrl == null) {
            return;
        }
        String classLocation = classUrl.toString();

        String baseName = classLocation.substring(0, classLocation.lastIndexOf(className) - 1);
        baseName = getWorkaroundPath(baseName);
        if (baseName.startsWith(URL_JAR_PREFIX)) {
            int lastIndex = baseName.lastIndexOf('/');
            if (lastIndex == -1) {
                lastIndex = baseName.lastIndexOf('\\');
            }
            if (lastIndex == -1) {
                return;
            }
            baseName = baseName.substring(URL_JAR_PREFIX.length(), lastIndex);
        } else if (baseName.startsWith(URL_FILE_PREFIX)) {
            baseName = baseName.substring(URL_FILE_PREFIX.length());
        } else {
            // Unhandled protocol
            return;
        }

        baseDir = new File(baseName);
    }

    private static URL getResourceUrl(String simpleName) {
        try {
            return BootstrapAgentEnvironment.class.getResource(simpleName);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getWorkaroundPath(String path) {

        StringBuilder builder = new StringBuilder();
        char c;
        for (int i = 0; i < path.length(); builder.append(c)) {
            c = path.charAt(i);
            if (c != '%') {
                i++;
                continue;
            }
            try {
                c = unescape(path, i);
                i += 3;
                if ((c & 128) == 0)
                    continue;
                switch (c >> 4) {
                    case 12: // '\f'
                    case 13: // '\r'
                        char c1 = unescape(path, i);
                        i += 3;
                        c = (char)((c & 31) << 6 | c1 & 63);
                        break;

                    case 14: // '\016'
                        char c2 = unescape(path, i);
                        i += 3;
                        char c3 = unescape(path, i);
                        i += 3;
                        c = (char)((c & 15) << 12 | (c2 & 63) << 6 | c3 & 63);
                        break;

                    default:
                        throw new IllegalArgumentException();
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }
        }

        return builder.toString();
    }

    private static char unescape(String s, int i) {
        return (char)Integer.parseInt(s.substring(i + 1, i + 3), 16);
    }

    private static boolean isLinux() {
        return OS_NAME.toLowerCase().startsWith("linux");
    }

    private static boolean isWindows() {
        return OS_NAME.toLowerCase().startsWith("win");
    }

    private static boolean isMacOS() {
        return OS_NAME.toLowerCase().startsWith("mac");
    }

    public static String[] getPlatformDescriptors() {
        if (isLinux()) {
            return new String[] { PLATFORM_DESCRIPTOR_LINUX_X86, PLATFORM_DESCRIPTOR_LINUX_X64 };
        } else if (isWindows()) {
            return new String[] { PLATFORM_DESCRIPTOR_WINDOWS, PLATFORM_DESCRIPTOR_WINDOWS_X64 };
        } else if (isMacOS()) {
            return new String[] { PLATFORM_DESCRIPTOR_MACOS };
        }
        return new String[0];
    }

}
