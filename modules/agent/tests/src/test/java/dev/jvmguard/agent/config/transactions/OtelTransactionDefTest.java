package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.comm.CodecTypes;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.transactions.MethodInterceptionTransactionDef.MethodInterceptionTransactionEnvironment;
import dev.jvmguard.agent.config.transactions.naming.AnnotationAttributeElement;
import dev.jvmguard.agent.config.transactions.naming.OtelSpanNameElement;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.MappedAnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.OtelAnnotationDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OtelTransactionDefTest {

    private static final String WITH_SPAN_DESCRIPTOR = "Lio/opentelemetry/instrumentation/annotations/WithSpan;";
    private static final String OBSERVED_DESCRIPTOR = "Lio/micrometer/observation/annotation/Observed;";

    @BeforeAll
    static void register() {
        CodecTypes.registerAll();
    }

    @Test
    void fixedAnnotationSet() {
        OtelTransactionDef def = new OtelTransactionDef();
        AnnotationDefinition[] definitions = def.getAnnotationDefinitions();
        assertEquals(3, definitions.length);

        OtelAnnotationDefinition withSpan = (OtelAnnotationDefinition)definitions[0];
        assertEquals(OtelAnnotationDefinition.Kind.WITH_SPAN, withSpan.getKind());
        assertEquals(WITH_SPAN_DESCRIPTOR, withSpan.getName());
        assertTrue(withSpan.isMethodAnnotation());
        assertEquals(TransactionType.OTEL, withSpan.getTransactionType());
        assertEquals("value", withSpan.getSpanNameAttribute());

        OtelAnnotationDefinition observedMethod = (OtelAnnotationDefinition)definitions[1];
        assertEquals(OtelAnnotationDefinition.Kind.OBSERVED, observedMethod.getKind());
        assertTrue(observedMethod.isMethodAnnotation());
        assertEquals("contextualName", observedMethod.getSpanNameAttribute());

        OtelAnnotationDefinition observedClass = (OtelAnnotationDefinition)definitions[2];
        assertEquals(OtelAnnotationDefinition.Kind.OBSERVED, observedClass.getKind());
        assertFalse(observedClass.isMethodAnnotation());

        // all definitions capture attributes for the span name naming
        for (AnnotationDefinition definition : definitions) {
            assertTrue(((OtelAnnotationDefinition)definition).isAttributeCapture());
        }
    }

    @Test
    void spanNameFilterIsResolvedPerAnnotation() {
        OtelTransactionDef def = new OtelTransactionDef();
        def.setAnnotationValueFilter(new AnnotationValueFilter("", "checkout*"));
        AnnotationDefinition[] definitions = def.getAnnotationDefinitions();

        MappedAnnotationDefinition withSpan = (MappedAnnotationDefinition)definitions[0];
        assertTrue(withSpan.matchesValueFilter(Map.of("value", "checkoutOrder")));
        assertFalse(withSpan.matchesValueFilter(Map.of("value", "other")));
        assertFalse(withSpan.matchesValueFilter(Map.of()));

        MappedAnnotationDefinition observed = (MappedAnnotationDefinition)definitions[1];
        assertTrue(observed.matchesValueFilter(Map.of("contextualName", "checkoutOrder")));
        assertFalse(observed.matchesValueFilter(Map.of("value", "checkoutOrder")));
    }

    @Test
    void defaultNamingUsesSpanNameWithFallback() {
        OtelTransactionDef def = new OtelTransactionDef();
        def.initDefault();
        List<NamingElement> elements = def.getNaming().getNamingElements();
        assertEquals(1, elements.size());
        assertInstanceOf(OtelSpanNameElement.class, elements.getFirst());

        assertEquals("checkout", name(def, Map.of("value", "checkout"), WITH_SPAN_DESCRIPTOR));
        assertEquals("order", name(def, Map.of("contextualName", "order"), OBSERVED_DESCRIPTOR));
        // empty or missing attribute falls back to ClassName.methodName
        assertEquals("Shop.checkout", name(def, Map.of(), WITH_SPAN_DESCRIPTOR));
        assertEquals("Shop.checkout", name(def, Map.of("value", ""), WITH_SPAN_DESCRIPTOR));
    }

    @Test
    void annotationAttributeElementNaming() {
        MappedTransactionDef def = new MappedTransactionDef();
        def.initDefault();
        def.getNaming().getNamingElements().clear();
        def.getNaming().getNamingElements().add(new AnnotationAttributeElement("operation"));

        assertTrue(((MappedAnnotationDefinition)def.getAnnotationDefinitions()[0]).isAttributeCapture());

        MethodInterceptionTransactionEnvironment environment =
            new MethodInterceptionTransactionEnvironment("com.example.Shop", "checkout", null, null, Map.of("operation", "place"), null);
        assertEquals("place", MethodInterceptionTransactionDef.getTransactionName(def.getNaming().getNamingElements(), environment));

        // absent attribute renders nothing
        environment = new MethodInterceptionTransactionEnvironment("com.example.Shop", "checkout", null, null, Map.of(), null);
        assertEquals("", MethodInterceptionTransactionDef.getTransactionName(def.getNaming().getNamingElements(), environment));
    }

    private static String name(OtelTransactionDef def, Map<String, String> attributes, String descriptor) {
        MethodInterceptionTransactionEnvironment environment =
            new MethodInterceptionTransactionEnvironment("com.example.Shop", "checkout", null, null, attributes, descriptor);
        return MethodInterceptionTransactionDef.getTransactionName(def.getNaming().getNamingElements(), environment);
    }

    @Test
    void binaryRoundTrip() throws Exception {
        TransactionSettings settings = new TransactionSettings();
        OtelTransactionDef otelDef = new OtelTransactionDef();
        otelDef.initDefault();
        otelDef.setAnnotationValueFilter(new AnnotationValueFilter("", "checkout*"));
        otelDef.setId(7L);
        settings.getTransactionDefs().add(otelDef);

        TransactionSettings read = roundTrip(settings, 2);
        assertEquals(1, read.getTransactionDefs().size());
        TransactionDef readDef = read.getTransactionDefs().getFirst();
        OtelTransactionDef readOtelDef = assertInstanceOf(OtelTransactionDef.class, readDef);
        assertEquals(7L, readOtelDef.getId());
        assertEquals(1, readOtelDef.getNaming().getNamingElements().size());
        assertInstanceOf(OtelSpanNameElement.class, readOtelDef.getNaming().getNamingElements().getFirst());
        assertNotNull(readOtelDef.getAnnotationValueFilter());
        assertEquals("checkout*", readOtelDef.getAnnotationValueFilter().getValue());
    }

    @Test
    void v1AgentDoesNotReceiveOtelDefsAndElements() throws Exception {
        TransactionSettings settings = new TransactionSettings();
        OtelTransactionDef otelDef = new OtelTransactionDef();
        otelDef.initDefault();
        settings.getTransactionDefs().add(otelDef);
        MappedTransactionDef mappedDef = new MappedTransactionDef();
        mappedDef.initDefault();
        mappedDef.getNaming().getNamingElements().add(new AnnotationAttributeElement("operation"));
        settings.getTransactionDefs().add(mappedDef);

        TransactionSettings read = roundTrip(settings, 1);
        // the OTel def is skipped entirely, the mapped def survives without the V2 naming element
        assertEquals(1, read.getTransactionDefs().size());
        MappedTransactionDef readMapped = assertInstanceOf(MappedTransactionDef.class, read.getTransactionDefs().getFirst());
        assertEquals(1, readMapped.getNaming().getNamingElements().size());
    }

    @Test
    void mappedDefFilterRoundTrip() throws Exception {
        TransactionSettings settings = new TransactionSettings();
        MappedTransactionDef mappedDef = new MappedTransactionDef();
        mappedDef.initDefault();
        mappedDef.setAnnotationValueFilter(new AnnotationValueFilter("operation", "place*"));
        settings.getTransactionDefs().add(mappedDef);

        TransactionSettings read = roundTrip(settings, 2);
        MappedTransactionDef readDef = assertInstanceOf(MappedTransactionDef.class, read.getTransactionDefs().getFirst());
        assertNotNull(readDef.getAnnotationValueFilter());
        assertEquals("operation", readDef.getAnnotationValueFilter().getAttributeName());
        assertEquals("place*", readDef.getAnnotationValueFilter().getValue());

        // V1 agents receive the def without the filter
        TransactionSettings readV1 = roundTrip(settings, 1);
        MappedTransactionDef readDefV1 = assertInstanceOf(MappedTransactionDef.class, readV1.getTransactionDefs().getFirst());
        assertNull(readDefV1.getAnnotationValueFilter());
    }

    @Test
    void automaticNameDescribesFilters() {
        OtelTransactionDef def = new OtelTransactionDef();
        assertEquals("*", def.getAutomaticName());

        def.setClassName("com.example.*");
        assertEquals("Classes: com.example.*", def.getAutomaticName());

        def.setClassName("*");
        def.setAnnotationValueFilter(new AnnotationValueFilter("", "checkout*"));
        assertEquals("Span name: checkout*", def.getAutomaticName());

        def.setClassName("com.example.*");
        assertEquals("Classes: com.example.*, Span name: checkout*", def.getAutomaticName());
    }

    @Test
    void mappedAutomaticNameDescribesFilters() {
        MappedTransactionDef def = new MappedTransactionDef();
        def.setAnnotationName("com.example.Traced");
        assertEquals("com.example.Traced [*]", def.getAutomaticName());

        def.setClassName("db.*");
        def.setAnnotationValueFilter(new AnnotationValueFilter("operation", "place*"));
        assertEquals("com.example.Traced [Classes: db.*, operation: place*]", def.getAutomaticName());
    }

    private static TransactionSettings roundTrip(TransactionSettings settings, int protocolVersion) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        settings.write(new CommunicationContext(protocolVersion), new DataOutputStream(buffer));
        TransactionSettings read = new TransactionSettings();
        read.read(new CommunicationContext(protocolVersion), new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));
        return read;
    }
}
