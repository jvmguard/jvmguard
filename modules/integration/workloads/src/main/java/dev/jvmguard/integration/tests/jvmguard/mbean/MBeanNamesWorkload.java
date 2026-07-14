package dev.jvmguard.integration.tests.jvmguard.mbean;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans.Test3;

import javax.management.*;
import javax.management.openmbean.OpenDataException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL") public class MBeanNamesWorkload extends AbstractJvmGuardRun {

    public static final String BEAN_PREFIX = "dev.jvmguard.test:type=Test";

    private static MBeanServer mbs1;
    private static MBeanServer mbs2;
    private static MBeanServer mbs3;

    private List<Object> beans = new ArrayList<Object>();

    @Override protected void work() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException, InstanceNotFoundException {
        for (int i = 0; i < 10; i++) {
            beans.add(new Test3(1));
        }

        mbs1 = MBeanServerFactory.createMBeanServer();
        mbs2 = MBeanServerFactory.createMBeanServer();
        mbs3 = MBeanServerFactory.createMBeanServer();
        mbs1.registerMBean(beans.get(0), new ObjectName(BEAN_PREFIX + 0));
        mbs1.registerMBean(beans.get(1), new ObjectName(BEAN_PREFIX + 1));
        mbs1.registerMBean(beans.get(2), new ObjectName(BEAN_PREFIX + 2));
        // The same ObjectName on a second server must be de-duplicated in the aggregated name list.
        mbs2.registerMBean(beans.get(3), new ObjectName(BEAN_PREFIX + 0));
        mbs3.registerMBean(beans.get(4), new ObjectName(BEAN_PREFIX + 3));

        next();

        // A bean registered after the initial enumeration must be picked up.
        mbs2.registerMBean(beans.get(5), new ObjectName(BEAN_PREFIX + 4));

        next();

        // wait for reconnect
        next();
    }

    protected void next() {
        waitForNextConfiguration();
        System.out.println("NEXT CONFIGURATION FOUND");
    }

    public static void main(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException, InstanceNotFoundException {
        new MBeanNamesWorkload().work();
    }
}
