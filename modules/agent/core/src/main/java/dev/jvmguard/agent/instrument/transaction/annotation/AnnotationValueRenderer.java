package dev.jvmguard.agent.instrument.transaction.annotation;

import org.objectweb.asm.Type;

/**
 * Renders ASM annotation attribute values to strings at instrumentation time, without loading
 * classes. Shared by the naming elements and the annotation value filter.
 */
public class AnnotationValueRenderer {

    private AnnotationValueRenderer() {
    }

    /**
     * Renders a scalar value passed to {@link org.objectweb.asm.AnnotationVisitor#visit}
     */
    public static String renderScalar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Type) {
            String className = ((Type)value).getClassName();
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }
        return value.toString();
    }
}
