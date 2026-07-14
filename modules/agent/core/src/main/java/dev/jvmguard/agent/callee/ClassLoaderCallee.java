package dev.jvmguard.agent.callee;

import dev.jvmguard.agent.instrument.TargetClassGenerator;
import dev.jvmguard.agent.servers.common.ServerClassLoader;

@SuppressWarnings("UnusedDeclaration")
public class ClassLoaderCallee {
    private static ClassLoader targetClassLoader;
    private static String targetPackage;
    private static Boolean jvmguardServer;

    static {
        targetClassLoader = TargetClassGenerator.getInstance().getClassLoader();
        targetPackage = TargetClassGenerator.getInstance().getClassPrefix();
    }

    public static boolean __jvmguard_useSystemClassLoader(Object thisObject, String name) {
        try {
            if (name == null) {
                return false;
            }

            if (targetPackage != null && name.startsWith(targetPackage)) {
                return thisObject != targetClassLoader;
            }
            if (thisObject instanceof ClassLoader) {
                return useSystemClassLoader(name);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean useSystemClassLoader(String name) {
        if (name.startsWith("dev.jvmguard.agent") && !name.startsWith(ServerClassLoader.SERVER_PACKAGE) && !name.startsWith("dev.jvmguard.agent.bootstrap.")) {
            if (jvmguardServer == null) {
                jvmguardServer = Boolean.getBoolean("jvmguard.server");
            }
            return !jvmguardServer;
        }
        return false;
    }

    public static Class __jvmguard_loadClass(Object classLoaderObject, String name) throws ClassNotFoundException {
        if (targetPackage != null && name.startsWith(targetPackage) && targetClassLoader != null) {
            return targetClassLoader.loadClass(name);
        }
        return Class.forName(name, true, null);
    }
}
