package dev.jvmguard.integration.tests.jvmguard.mbean;

import com.sun.jmx.mbeanserver.JmxMBeanServer;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

public class TestBuilder extends MBeanServerBuilder {
    @Override
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
        System.out.println("USING TEST BUILDER");
        return JmxMBeanServer.newMBeanServer(defaultDomain, outer, delegate, false);
    }
}
