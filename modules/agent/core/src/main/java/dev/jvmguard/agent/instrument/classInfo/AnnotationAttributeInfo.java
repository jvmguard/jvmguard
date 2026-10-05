package dev.jvmguard.agent.instrument.classInfo;

import java.util.Map;

/**
 * A class-level annotation whose attribute values were captured at instrumentation time, stored in
 * {@link ClassFileInfo#setClassAnnotations(Object[])} instead of the plain descriptor string.
 */
public class AnnotationAttributeInfo {

    private final String descriptor;
    private final Map<String, String> attributes;

    public AnnotationAttributeInfo(String descriptor, Map<String, String> attributes) {
        this.descriptor = descriptor;
        this.attributes = attributes;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return descriptor + attributes;
    }
}
