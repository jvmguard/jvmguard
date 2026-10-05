package dev.jvmguard.agent.instrument.transaction.annotation;

import dev.jvmguard.agent.config.transactions.TransactionType;

import java.util.Objects;

/**
 * Annotation definition for the fixed set of tracing annotations handled by
 * {@link dev.jvmguard.agent.config.transactions.OtelTransactionDef}. Adds the annotation kind,
 * which drives the span-name attribute resolution and the dedup rule (a WithSpan interception
 * beats an Observed interception on the same method).
 */
public class OtelAnnotationDefinition extends MappedAnnotationDefinition {

    public static final String WITH_SPAN_ANNOTATION = "io.opentelemetry.instrumentation.annotations.WithSpan";
    public static final String OBSERVED_ANNOTATION = "io.micrometer.observation.annotation.Observed";

    private static final String WITH_SPAN_DESCRIPTOR = "L" + WITH_SPAN_ANNOTATION.replace('.', '/') + ";";
    private static final String OBSERVED_DESCRIPTOR = "L" + OBSERVED_ANNOTATION.replace('.', '/') + ";";

    private final Kind kind;

    public OtelAnnotationDefinition(Kind kind, boolean classLevel, TransactionType transactionType) {
        super(kind == Kind.WITH_SPAN ? WITH_SPAN_ANNOTATION : OBSERVED_ANNOTATION, !classLevel, !classLevel, transactionType);
        this.kind = kind;
        // same flags as the former built-in mapped definitions
        implementingOnly(!classLevel);
        attributeCapture(true);
    }

    public Kind getKind() {
        return kind;
    }

    public String getSpanNameAttribute() {
        return kind == Kind.WITH_SPAN ? "value" : "contextualName";
    }

    /**
     * Resolves the span-name attribute from an annotation descriptor, or null for unknown
     * annotations.
     */
    public static String getSpanNameAttribute(String annotationDescriptor) {
        if (WITH_SPAN_DESCRIPTOR.equals(annotationDescriptor)) {
            return "value";
        } else if (OBSERVED_DESCRIPTOR.equals(annotationDescriptor)) {
            return "contextualName";
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        OtelAnnotationDefinition that = (OtelAnnotationDefinition)o;
        return kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), kind);
    }

    public enum Kind {
        WITH_SPAN,
        OBSERVED
    }
}
