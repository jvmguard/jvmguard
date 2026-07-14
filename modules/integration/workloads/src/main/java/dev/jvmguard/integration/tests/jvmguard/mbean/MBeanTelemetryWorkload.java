package dev.jvmguard.integration.tests.jvmguard.mbean;

import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.Util;
import dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans.*;

import javax.management.*;
import javax.management.openmbean.OpenDataException;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("ALL") public class MBeanTelemetryWorkload extends AbstractJvmGuardRun {

    static final String COM_JVMGUARD_TEST = "dev.jvmguard.test:";
    static final String TEST1_BEAN_NAME = COM_JVMGUARD_TEST + "type=Test1";
    static final String TEST2_BEAN_NAME = COM_JVMGUARD_TEST + "type=Test2";
    static final String TEST_BOTH_BEAN_NAME = COM_JVMGUARD_TEST + "type=TestBoth";
    static final String TEST_COMPLEX = COM_JVMGUARD_TEST + "type=TestComplex";

    private static final String JAVAX_MANAGEMENT_BUILDER_INITIAL = "javax.management.builder.initial";
    private MBeanServer mbs1;
    private MBeanServer mbs2;

    @Override
    protected void work() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        mbs1 = MBeanServerFactory.newMBeanServer();
        mbs1.registerMBean(new Test3(100), new ObjectName(TEST1_BEAN_NAME));
        mbs1.registerMBean(new Test3(1000), new ObjectName(TEST_BOTH_BEAN_NAME));

        if (!Util.isJava9Plus()) {
            System.setProperty(JAVAX_MANAGEMENT_BUILDER_INITIAL, TestBuilder.class.getName());
        }
        mbs2 = MBeanServerFactory.newMBeanServer();
        mbs2.registerMBean(new Test3(200), new ObjectName(TEST2_BEAN_NAME));
        mbs2.registerMBean(new Test3(2000), new ObjectName(TEST_BOTH_BEAN_NAME));

        Complex1Sub[] subArray = {new Complex1Sub(1, "a1"), new Complex1Sub(2, "a2")};
        HashMap<String, Complex1Sub> singleMap = new HashMap<String, Complex1Sub>();
        for (Complex1Sub complex1Sub : subArray) {
            singleMap.put("key\\" + complex1Sub.getSub2() + "/", complex1Sub);
        }
        HashMap<Complex1Sub, Complex1Sub> doubleMap = new HashMap<Complex1Sub, Complex1Sub>();
        for (Complex1Sub complex1Sub : subArray) {
            doubleMap.put(new Complex1Sub(complex1Sub.getSub1(), "key" + complex1Sub.getSub2()), complex1Sub);
        }
        Complex1 complex1 = new Complex1(new Measure("bytes", 33L), 1, new long[]{1, 10, 100}, new Complex1Sub(1, "single"), subArray, Arrays.asList(subArray), doubleMap, singleMap);

        mbs2.registerMBean(new Test1(null, complex1), new ObjectName(TEST_COMPLEX));

        waitForNextConfiguration();
        System.out.println("NEXT CONFIGURATION FOUND");
    }

    public static void main(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        new MBeanTelemetryWorkload().work();
    }
}
