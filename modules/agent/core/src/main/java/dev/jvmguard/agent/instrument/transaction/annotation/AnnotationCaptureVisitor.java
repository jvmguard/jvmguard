package dev.jvmguard.agent.instrument.transaction.annotation;

import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;

/**
 * Captures all explicitly set attributes of an annotation into a map of rendered string values.
 * Nested annotations are not captured. The map is passed to {@link #onEnd(Map)} at visitEnd.
 */
public abstract class AnnotationCaptureVisitor extends AnnotationVisitor {

    private final Map<String, String> attributes = new HashMap<>();

    public AnnotationCaptureVisitor() {
        super(ASM_VERSION);
    }

    protected abstract void onEnd(Map<String, String> attributes);

    @Override
    public void visit(String name, Object value) {
        String rendered = AnnotationValueRenderer.renderScalar(value);
        if (rendered != null) {
            attributes.put(name, rendered);
        }
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        attributes.put(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        return new AnnotationVisitor(ASM_VERSION) {
            private final List<String> elements = new ArrayList<>();

            @Override
            public void visit(String elementName, Object value) {
                String rendered = AnnotationValueRenderer.renderScalar(value);
                if (rendered != null) {
                    elements.add(rendered);
                }
            }

            @Override
            public void visitEnum(String elementName, String descriptor, String value) {
                elements.add(value);
            }

            @Override
            public void visitEnd() {
                attributes.put(name, String.join(", ", elements));
            }
        };
    }

    @Override
    public void visitEnd() {
        onEnd(attributes);
    }
}
