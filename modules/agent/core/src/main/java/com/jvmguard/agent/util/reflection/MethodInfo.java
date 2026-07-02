package com.jvmguard.agent.util.reflection;

import java.lang.reflect.Method;

public class MethodInfo {
    public final Method method;

    public MethodInfo(Method method) {
        this.method = method;
    }

    public Object invokeIfPossible(Object obj, Object... params) {
        if (method != null) {
            try {
                return method.invoke(obj, params);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

}
