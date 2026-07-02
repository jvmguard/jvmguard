package com.jvmguard.agent.servers.common;

import com.jvmguard.agent.callee.ClassLoaderCallee;

import java.net.URL;
import java.net.URLClassLoader;

public class ServerClassLoader extends URLClassLoader {
    public static final String SERVER_PACKAGE = "com.jvmguard.agent.servers";

    private ServerClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(SERVER_PACKAGE)) {
            // First, check if the class has already been loaded
            Class c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        } else if (ClassLoaderCallee.useSystemClassLoader(name)) {
            return Class.forName(name, true, null);
        } else {
            return super.loadClass(name, resolve);
        }
    }
}
