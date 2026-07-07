package com.jvmguard.agent.instrument.transaction.annotation;

import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.NoTransaction;
import com.jvmguard.annotation.Telemetry;

public class DeclaredAnnotationDefinition extends AnnotationDefinition {
    private static final String ANNOTATION_PACKAGE = System.getProperty("jvmguard.annotationPackage", "com.jvmguard.annotation").replace('.', '/');

    public static final String CLASS_TRANSACTION_DESCRIPTOR;
    public static final String METHOD_TRANSACTION_DESCRIPTOR;
    public static final String NO_TRANSACTION_DESCRIPTOR;
    public static final String TELEMETRY_DESCRIPTOR;

    static {
        if (Boolean.getBoolean("jvmguard.server") || Boolean.getBoolean("jvmguard.obfuscated")) {
            CLASS_TRANSACTION_DESCRIPTOR = "L" + System.getProperty("jvmguard.int.declaredClass", ClassTransaction.class.getName()).replace('.', '/') + ";";
            METHOD_TRANSACTION_DESCRIPTOR = "L" + System.getProperty("jvmguard.int.declaredMethod", MethodTransaction.class.getName()).replace('.', '/') + ";";
            NO_TRANSACTION_DESCRIPTOR = "L" + System.getProperty("jvmguard.int.declaredNo", NoTransaction.class.getName()).replace('.', '/') + ";";
            TELEMETRY_DESCRIPTOR = "L" + System.getProperty("jvmguard.int.declaredTelemetry", Telemetry.class.getName()).replace('.', '/') + ";";
        } else {
            CLASS_TRANSACTION_DESCRIPTOR = "L" + ANNOTATION_PACKAGE + "/ClassTransaction;";
            METHOD_TRANSACTION_DESCRIPTOR = "L" + ANNOTATION_PACKAGE + "/MethodTransaction;";
            NO_TRANSACTION_DESCRIPTOR = "L" + ANNOTATION_PACKAGE + "/NoTransaction;";
            TELEMETRY_DESCRIPTOR = "L" + ANNOTATION_PACKAGE + "/Telemetry;";
        }
    }

    public DeclaredAnnotationDefinition(String name, boolean methodAnnotation, String groupName) {
        super(name + groupName, methodAnnotation, true);
    }

    @Override
    public String getUsedAnnotationDescriptor(SearchType searchType) {
        if (isMethodAnnotation()) {
            if (searchType == SearchType.METHOD || searchType == SearchType.INHERITED_METHOD) {
                return getName();
            }
        } else {
            if (searchType == SearchType.CLASS || searchType == SearchType.INHERITED_CLASS) {
                return getName();
            }
        }
        return null;
    }

    public static boolean isDeclaredDescriptor(String fullName) {
        return fullName.startsWith(METHOD_TRANSACTION_DESCRIPTOR) || fullName.startsWith(CLASS_TRANSACTION_DESCRIPTOR);
    }

    public static String getMatchAllDescriptor(String fullName) {
        if (fullName.startsWith(METHOD_TRANSACTION_DESCRIPTOR)) {
            return METHOD_TRANSACTION_DESCRIPTOR;
        } else if (fullName.startsWith(CLASS_TRANSACTION_DESCRIPTOR)) {
            return CLASS_TRANSACTION_DESCRIPTOR;
        }
        return null;
    }

    @Override
    public boolean isInheritable() {
        return false; // inheritable declared transactions are handled separately
    }
}
