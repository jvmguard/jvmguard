package com.jvmguard.integration.tests.jvmguard.mbean;

import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.mbean.dynamic.TestDynamic;
import com.jvmguard.integration.tests.jvmguard.mbean.mxbeans.*;
import com.jvmguard.integration.tests.jvmguard.mbean.standard.TestStandard;

import javax.management.*;
import javax.management.openmbean.OpenDataException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("ALL") public class MBeanWorkload extends AbstractJvmGuardRun {

    static final String COM_JVMGUARD_TEST = "com.jvmguard.test:";
    static final String DYNAMIC_BEAN_NAME = COM_JVMGUARD_TEST + "type=Dynamic";
    static final String STANDARD_BEAN_NAME = COM_JVMGUARD_TEST + "type=Standard";
    static final String CHILD_BEAN_NAME = COM_JVMGUARD_TEST + "type=Child";
    static final String PARENT_BEAN_NAME = COM_JVMGUARD_TEST + "type=Parent";
    static final String TEST2_BEAN_NAME = COM_JVMGUARD_TEST + "type=Test2";

    @Override
    protected void work() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        Complex1Sub[] subArray = {new Complex1Sub(1, "a1"), new Complex1Sub(2, "a2")};
        HashMap<String, Complex1Sub> singleMap = new HashMap<String, Complex1Sub>();
        for (Complex1Sub complex1Sub : subArray) {
            singleMap.put("key" + complex1Sub.getSub2(), complex1Sub);
        }
        HashMap<Complex1Sub, Complex1Sub> doubleMap = new HashMap<Complex1Sub, Complex1Sub>();
        for (Complex1Sub complex1Sub : subArray) {
            doubleMap.put(new Complex1Sub(complex1Sub.getSub1(), "key" + complex1Sub.getSub2()), complex1Sub);
        }
        Complex1 complex1 = new Complex1(new Measure("bytes", 33L), 1, new long[]{1, 10, 100}, new Complex1Sub(1, "single"), subArray, Arrays.asList(subArray), doubleMap, singleMap);

        Test1 parent = new Test1(null, null);
        mbs.registerMBean(parent, new ObjectName(PARENT_BEAN_NAME));
        mbs.registerMBean(new Test1(parent, complex1), new ObjectName(CHILD_BEAN_NAME));

        mbs.registerMBean(new TestStandard(), new ObjectName(STANDARD_BEAN_NAME));
        mbs.registerMBean(new TestDynamic(), new ObjectName(DYNAMIC_BEAN_NAME));

        mbs.registerMBean(new Test2(), new ObjectName(TEST2_BEAN_NAME));

        waitForNextConfiguration();
        System.out.println("NEXT CONFIGURATION FOUND");
    }

    public static void main(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, OpenDataException {
        new MBeanWorkload().work();
    }
}
