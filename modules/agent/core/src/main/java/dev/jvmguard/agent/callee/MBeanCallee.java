package dev.jvmguard.agent.callee;

import dev.jvmguard.mbean.data.MBeanManager;

import javax.management.MBeanServer;

@SuppressWarnings("UnusedDeclaration")
public class MBeanCallee {

    public static void __jvmguard_mBeanServerCreated(MBeanServer mBeanServer) {
        try {
            MBeanManager.addServer(mBeanServer);
        } catch (Throwable ignored) {
        }
    }
}
