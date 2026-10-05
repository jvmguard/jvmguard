package dev.jvmguard.agent.instrument.transaction.annotation;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnotationValueRendererTest {

    @Test
    void stringsAreRenderedAsIs() {
        assertEquals("checkout", AnnotationValueRenderer.renderScalar("checkout"));
        assertEquals("", AnnotationValueRenderer.renderScalar(""));
    }

    @Test
    void primitivesAreRenderedViaToString() {
        assertEquals("5", AnnotationValueRenderer.renderScalar(5));
        assertEquals("true", AnnotationValueRenderer.renderScalar(Boolean.TRUE));
        assertEquals("c", AnnotationValueRenderer.renderScalar('c'));
        assertEquals("1.5", AnnotationValueRenderer.renderScalar(1.5d));
    }

    @Test
    void classesAreRenderedWithSimpleName() {
        assertEquals("String", AnnotationValueRenderer.renderScalar(Type.getType(String.class)));
        assertEquals("Map$Entry", AnnotationValueRenderer.renderScalar(Type.getObjectType("java/util/Map$Entry")));
    }

    @Test
    void nullIsRenderedAsNull() {
        assertNull(AnnotationValueRenderer.renderScalar(null));
    }
}
