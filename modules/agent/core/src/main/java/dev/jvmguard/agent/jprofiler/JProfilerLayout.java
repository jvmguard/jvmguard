package dev.jvmguard.agent.jprofiler;

import java.io.File;

/**
 * Resolves the JProfiler home directory inside an extracted agent package.
 */
public class JProfilerLayout {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    private static final String JPENABLE = IS_WINDOWS ? "jpenable.exe" : "jpenable";

    /** The JProfiler home directory (containing {@code bin/}, {@code lib/}) within {@code installDir}, or null. */
    public static File resolveHome(File installDir) {
        if (isHome(installDir)) {
            return installDir;
        }
        File[] children = installDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && isHome(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    public static File jpenable(File home) {
        return new File(new File(home, "bin"), JPENABLE);
    }

    private static boolean isHome(File dir) {
        return jpenable(dir).isFile();
    }
}
