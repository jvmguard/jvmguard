package com.jvmguard.agent.helper;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.util.reflection.ReflectionUtil;
import com.jvmguard.agent.util.Logger;

import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DiagnosticMbeanHandler {
    private static final DynamicMBean M_BEAN;
    private static final MBeanInfo INFO;

    public static final String[] SIGNATURE = new String[] {String[].class.getName()};

    static {
        DynamicMBean mbean;
        MBeanInfo mBeanInfo;
        try {
            Method getDiagnosticCommandMBean;
            try {
                //noinspection JavaReflectionMemberAccess
                getDiagnosticCommandMBean = Class.forName("sun.management.ManagementFactoryHelper").getMethod("getDiagnosticCommandMBean");
            } catch (Throwable t) {
                getDiagnosticCommandMBean = Class.forName("com.sun.management.internal.DiagnosticCommandImpl").getDeclaredMethod("getDiagnosticCommandMBean");
            }
            ReflectionUtil.setAccessible(getDiagnosticCommandMBean, true);
            mbean = (DynamicMBean)getDiagnosticCommandMBean.invoke(null);
            mBeanInfo = mbean.getMBeanInfo();
            if (mBeanInfo == null) {
                throw new RuntimeException("no mbean info found");
            }
        } catch (Throwable e) {
            Logger.log(Subsystem.COMMON, 2, false, "DiagnosticCommand MBean not available. Using attach API. (" + e + ")");
            Logger.log(Subsystem.COMMON, 2, false, e);
            mbean = null;
            mBeanInfo = null;
        }
        M_BEAN = mbean;
        INFO = mBeanInfo;
    }

    public static boolean isMBeanAvailable(String... operations) {
        if (INFO != null) {
            try {
                DiagnosticMbeanHandler.checkOperations(operations);
                return true;
            } catch (Throwable e) {
                Logger.log(Subsystem.COMMON, 2, false, "DiagnosticCommand MBean operations " + Arrays.toString(operations) + " not available. Using attach API. (" + e + ")");
                Logger.log(Subsystem.COMMON, 2, false, e);
            }
        }
        return false;
    }

    private static boolean compareSignature(MBeanOperationInfo operationInfo) {
        if (!operationInfo.getReturnType().equals(String.class.getName())) {
            return false;
        }
        MBeanParameterInfo[] signature = operationInfo.getSignature();
        if (signature.length != SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (!SIGNATURE[i].equals(signature[i].getType())) {
                return false;
            }
        }
        return true;
    }

    private static void checkOperations(String... names) {
        if (INFO == null) {
            throw new RuntimeException("diagnostic bean not available");
        }
        Set<String> nameSet = new HashSet<>(Arrays.asList(names));
        for (MBeanOperationInfo operationInfo : INFO.getOperations()) {
            if (operationInfo.getName() != null && nameSet.contains(operationInfo.getName()) && DiagnosticMbeanHandler.compareSignature(operationInfo)) {
                nameSet.remove(operationInfo.getName());
            }
        }
        if (!nameSet.isEmpty()) {
            throw new RuntimeException("diagnostic operation not found " + nameSet);
        }
    }

    public static String invoke(String operationName, String... arguments) throws Throwable {
        try {
            return (String)M_BEAN.invoke(operationName, new Object[] {arguments}, DiagnosticMbeanHandler.SIGNATURE);
        } catch (Throwable e) {
            while (e.getCause() != null) {
                e = e.getCause();
            }
            throw e;
        }
    }
}
