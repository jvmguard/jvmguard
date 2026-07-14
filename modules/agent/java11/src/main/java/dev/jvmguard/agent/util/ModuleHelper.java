package dev.jvmguard.agent.util;

import java.util.concurrent.ConcurrentHashMap;

public class ModuleHelper {

    private static final int OPENED = 0x1;
    private static final int EXPORTED = 0x2;

    private static final ConcurrentHashMap<String, Integer> packageToState = new ConcurrentHashMap<>();

    private static volatile ModuleHelperImplementation impl = new NoopModuleHelperImpl();

    public static void setImplementation(ModuleHelperImplementation impl) {
        ModuleHelper.impl = impl;
    }

    public static void addOpens(Class clazz, String packageName) {
        int state = getState(packageName);
        if ((state & OPENED) == 0) {
            impl.addOpens(clazz, packageName); // may be called more than once because method is not synchronized, but that should be no problem
            packageToState.put(packageName, state | OPENED);
        }
    }

    public static void addExports(Class clazz, String packageName) {
        int state = getState(packageName);
        if ((state & EXPORTED) == 0) {
            impl.addExports(clazz, packageName);
            packageToState.put(packageName, state | EXPORTED);
        }
    }

    private static int getState(String packageName) {
        Integer state = packageToState.get(packageName);
        return state == null ? 0 : state;
    }

    public interface ModuleHelperImplementation {
        void addOpens(Class clazz, String packageName);
        void addExports(Class clazz, String packageName);
    }

    private static class NoopModuleHelperImpl implements ModuleHelperImplementation {
        @Override
        public void addOpens(Class clazz, String packageName) {
        }

        @Override
        public void addExports(Class clazz, String packageName) {
        }

    }
}
