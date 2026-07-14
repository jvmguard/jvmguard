package dev.jvmguard.integration.tests.jvmguard.mbean;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.Util;
import dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans.Test3;

import javax.management.*;
import javax.management.openmbean.OpenDataException;

@SuppressWarnings("ALL") public class MBeanServerWorkload extends AbstractJvmGuardRun {

    static final String COM_JVMGUARD_TEST = "dev.jvmguard.test:";
    static final String TEST1_BEAN_NAME = COM_JVMGUARD_TEST + "type=Test1";
    static final String TEST2_BEAN_NAME = COM_JVMGUARD_TEST + "type=Test2";
    static final String TEST_BOTH_BEAN_NAME = COM_JVMGUARD_TEST + "type=TestBoth";

    private static final String JAVAX_MANAGEMENT_BUILDER_INITIAL = "javax.management.builder.initial";
    private MBeanServer mbs1;
    private MBeanServer mbs2;

    @Override
    protected void work() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        mbs1 = MBeanServerFactory.newMBeanServer();
        mbs1.registerMBean(new Test3(1), new ObjectName(TEST1_BEAN_NAME));
        mbs1.registerMBean(new Test3(10), new ObjectName(TEST_BOTH_BEAN_NAME));

        if (!Util.isJava9Plus()) {
            System.setProperty(JAVAX_MANAGEMENT_BUILDER_INITIAL, TestBuilder.class.getName());
        }
        mbs2 = MBeanServerFactory.newMBeanServer();
        mbs2.registerMBean(new Test3(2), new ObjectName(TEST2_BEAN_NAME));
        mbs2.registerMBean(new Test3(20), new ObjectName(TEST_BOTH_BEAN_NAME));

        waitForNextConfiguration();
        System.out.println("NEXT CONFIGURATION FOUND");
    }

    public static void main(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        new MBeanServerWorkload().work();
    }
}
