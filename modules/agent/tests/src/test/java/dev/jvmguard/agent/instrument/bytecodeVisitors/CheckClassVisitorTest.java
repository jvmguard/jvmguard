package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.config.transactions.AnnotatedTransactionDef;
import dev.jvmguard.agent.config.transactions.AnnotationValueFilter;
import dev.jvmguard.agent.config.transactions.MappedTransactionDef;
import dev.jvmguard.agent.config.transactions.OtelTransactionDef;
import dev.jvmguard.agent.config.transactions.naming.AnnotationAttributeElement;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.interceptions.AnnotationInterception;
import dev.jvmguard.agent.instrument.interceptions.BaseInterception;
import dev.jvmguard.agent.instrument.interceptions.TransactionInterception;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import dev.jvmguard.agent.instrument.transaction.annotation.OtelAnnotationDefinition;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

class CheckClassVisitorTest {

    private static final String TRACED_DESCRIPTOR = "Ltest/Traced;";
    private static final String WITH_SPAN_DESCRIPTOR = "Lio/opentelemetry/instrumentation/annotations/WithSpan;";
    private static final String OBSERVED_DESCRIPTOR = "Lio/micrometer/observation/annotation/Observed;";

    //
    // inheritable method annotations with an annotation value filter
    //

    @Test
    void valueFilterOnInheritableMethodAnnotationKeepsOnlyMatchingMethods() {
        Instrumenter instrumenter = new Instrumenter();
        MappedTransactionDef def = mappedMethodDef(true);
        def.setAnnotationValueFilter(new AnnotationValueFilter("value", "gold*"));
        installDefs(instrumenter, def);

        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Base", null, null, new MethodSpec("gold", TRACED_DESCRIPTOR, "gold"), new MethodSpec("silver", TRACED_DESCRIPTOR, "silver")));

        // the filter must not silently drop the whole definition: exactly the method whose attribute
        // value matches is intercepted, with its captured attributes
        assertEquals(1, visitor.getClassInterceptions().size());
        AnnotationInterception interception = (AnnotationInterception)visitor.getClassInterceptions().iterator().next();
        assertEquals(Map.of("value", "gold"), interception.getAnnotationAttributes());
        assertEquals(Set.of(new InterceptionMethod("gold", "()V")), interception.getDefinedMethods());
    }

    @Test
    void inheritableMethodAnnotationsAreGroupedByAttributeValues() {
        Instrumenter instrumenter = new Instrumenter();
        MappedTransactionDef def = mappedMethodDef(true);
        // an annotation attribute naming element forces attribute capture
        def.getNaming().getNamingElements().add(new AnnotationAttributeElement("value"));
        installDefs(instrumenter, def);

        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Base", null, null, new MethodSpec("gold", TRACED_DESCRIPTOR, "gold"), new MethodSpec("silver", TRACED_DESCRIPTOR, "silver")));

        Map<Set<InterceptionMethod>, Map<String, String>> methodsToAttributes = new HashMap<>();
        for (BaseInterception interception : visitor.getClassInterceptions()) {
            AnnotationInterception annotationInterception = (AnnotationInterception)interception;
            methodsToAttributes.put(annotationInterception.getDefinedMethods(), annotationInterception.getAnnotationAttributes());
        }
        assertEquals(Map.of(
            Set.of(new InterceptionMethod("gold", "()V")), Map.of("value", "gold"),
            Set.of(new InterceptionMethod("silver", "()V")), Map.of("value", "silver")), methodsToAttributes);
    }

    @Test
    void valueFilterOnInheritableMethodAnnotationAppliesToSubclasses() {
        Instrumenter instrumenter = new Instrumenter();
        MappedTransactionDef def = mappedMethodDef(true);
        def.setAnnotationValueFilter(new AnnotationValueFilter("value", "gold*"));
        installDefs(instrumenter, def);

        check(instrumenter, classBytes("test/Base", null, null, new MethodSpec("gold", TRACED_DESCRIPTOR, "gold")));
        CheckClassVisitor subVisitor = check(instrumenter, classBytes("test/Sub", "test/Base", null, new MethodSpec("gold", null, null)));

        // the overriding method inherits the interception of the annotated base method, with the
        // attributes captured on the base class
        assertEquals(1, subVisitor.getClassInterceptions().size());
        AnnotationInterception interception = (AnnotationInterception)subVisitor.getClassInterceptions().iterator().next();
        assertEquals("test.Base", interception.getDeclaringClassName());
        assertEquals(Map.of("value", "gold"), interception.getAnnotationAttributes());
        assertEquals(Set.of(new InterceptionMethod("gold", "()V")), interception.getDefinedMethods());
    }

    @Test
    void valueFilterOnNonInheritableMethodAnnotationFiltersAtCaptureTime() {
        Instrumenter instrumenter = new Instrumenter();
        MappedTransactionDef def = mappedMethodDef(false);
        def.setAnnotationValueFilter(new AnnotationValueFilter("value", "gold*"));
        installDefs(instrumenter, def);

        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Base", null, null, new MethodSpec("gold", TRACED_DESCRIPTOR, "gold"), new MethodSpec("silver", TRACED_DESCRIPTOR, "silver")));

        assertEquals(Set.of(new InterceptionMethod("gold", "()V")), visitor.getMethodInterceptions().keySet());
    }

    @Test
    void staleAttributeCacheDoesNotHideMethodsAddedAfterCaptureStopped() {
        Instrumenter instrumenter = new Instrumenter();
        MappedTransactionDef def = mappedMethodDef(true);
        def.setAnnotationValueFilter(new AnnotationValueFilter("value", "gold*"));
        installDefs(instrumenter, def);

        // pass 1: the filter is active, so attributes are captured and stored per method
        check(instrumenter, classBytes("test/Base", null, null, new MethodSpec("gold", TRACED_DESCRIPTOR, "gold")));

        // pass 2: the filter is removed, so no attributes are captured anymore, and the class has
        // gained another annotated method - the full stored method set must be used
        def.setAnnotationValueFilter(null);
        installDefs(instrumenter, def);
        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Base", null, null,
            new MethodSpec("gold", TRACED_DESCRIPTOR, "gold"),
            new MethodSpec("silver", TRACED_DESCRIPTOR, "silver")));

        assertEquals(1, visitor.getClassInterceptions().size());
        AnnotationInterception interception = (AnnotationInterception)visitor.getClassInterceptions().iterator().next();
        assertEquals(Set.of(new InterceptionMethod("gold", "()V"), new InterceptionMethod("silver", "()V")),
            interception.getDefinedMethods());
    }

    //
    // the "WithSpan wins over Observed" dedup rule for class-level @Observed
    //

    @Test
    void classLevelObservedSkipsMethodsWithWithSpan() {
        Instrumenter instrumenter = new Instrumenter();
        installDefs(instrumenter, new OtelTransactionDef());

        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Mixed", null, OBSERVED_DESCRIPTOR,
            new MethodSpec("withSpan", WITH_SPAN_DESCRIPTOR, null),
            new MethodSpec("plain", null, null)));

        // the method-level @WithSpan interception exists ...
        TransactionInterception withSpanInterception = findSingleMethodInterception(visitor);
        assertEquals(OtelAnnotationDefinition.Kind.WITH_SPAN, ((OtelAnnotationDefinition)withSpanInterception.getDefinition()).getKind());

        // ... and the class-level @Observed interception is restricted to the remaining public methods
        AnnotationInterception observedInterception = findSingleClassInterception(visitor);
        assertEquals(OtelAnnotationDefinition.Kind.OBSERVED, ((OtelAnnotationDefinition)observedInterception.getDefinition()).getKind());
        assertEquals(Set.of(new InterceptionMethod("plain", "()V")), observedInterception.getDefinedMethods());
    }

    @Test
    void classLevelObservedSkipsMethodsWithMethodLevelObserved() {
        Instrumenter instrumenter = new Instrumenter();
        installDefs(instrumenter, new OtelTransactionDef());

        CheckClassVisitor visitor = check(instrumenter, classBytes("test/Mixed", null, OBSERVED_DESCRIPTOR,
            new MethodSpec("observed", OBSERVED_DESCRIPTOR, null),
            new MethodSpec("plain", null, null)));

        TransactionInterception methodInterception = findSingleMethodInterception(visitor);
        assertEquals(OtelAnnotationDefinition.Kind.OBSERVED, ((OtelAnnotationDefinition)methodInterception.getDefinition()).getKind());

        AnnotationInterception observedInterception = findSingleClassInterception(visitor);
        assertEquals(Set.of(new InterceptionMethod("plain", "()V")), observedInterception.getDefinedMethods());
    }

    //
    // helpers
    //

    private static MappedTransactionDef mappedMethodDef(boolean interceptSubclasses) {
        MappedTransactionDef def = new MappedTransactionDef();
        def.setAnnotationName("test/Traced");
        def.setAnnotatedTarget(MappedTransactionDef.AnnotatedTarget.METHOD);
        def.setInterceptSubclasses(interceptSubclasses);
        return def;
    }

    private static void installDefs(Instrumenter instrumenter, AnnotatedTransactionDef def) {
        Map<String, List<AnnotationTransactionDefList>> annotationDefinitions = new HashMap<>();
        Set<String> inheritableMethodAnnotations = new HashSet<>();
        for (AnnotationDefinition definition : def.getAnnotationDefinitions()) {
            AnnotationTransactionDefList transactionDefList = AnnotationTransactionDefList.create(definition);
            transactionDefList.addTransactionDef(def);
            // several definitions can share a descriptor, e.g. method-level and class-level @Observed
            annotationDefinitions.computeIfAbsent(definition.getName(), _ -> new ArrayList<>()).add(transactionDefList);
            if (definition.isMethodAnnotation() && definition.isInheritable()) {
                inheritableMethodAnnotations.add(definition.getName());
            }
        }
        instrumenter.setAnnotationDefinitions(annotationDefinitions, inheritableMethodAnnotations);
    }

    private static CheckClassVisitor check(Instrumenter instrumenter, byte[] classFileBuffer) {
        CheckClassVisitor checkClassVisitor = new CheckClassVisitor(instrumenter, false, true);
        new org.objectweb.asm.ClassReader(classFileBuffer).accept(checkClassVisitor,
            org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        return checkClassVisitor;
    }

    private static TransactionInterception findSingleMethodInterception(CheckClassVisitor visitor) {
        assertEquals(1, visitor.getMethodInterceptions().size());
        Set<BaseInterception> interceptions = visitor.getMethodInterceptions().values().iterator().next();
        assertEquals(1, interceptions.size());
        return (TransactionInterception)interceptions.iterator().next();
    }

    private static AnnotationInterception findSingleClassInterception(CheckClassVisitor visitor) {
        assertEquals(1, visitor.getClassInterceptions().size());
        return (AnnotationInterception)visitor.getClassInterceptions().iterator().next();
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class MethodSpec {
        final String name;
        final String annotationDescriptor;
        final String annotationValue;

        MethodSpec(String name, String annotationDescriptor, String annotationValue) {
            this.name = name;
            this.annotationDescriptor = annotationDescriptor;
            this.annotationValue = annotationValue;
        }
    }

    private static byte[] classBytes(String className, String superClassName, String classAnnotationDescriptor, MethodSpec... methods) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(V1_8, ACC_PUBLIC, className, null, superClassName != null ? superClassName : "java/lang/Object", null);
        if (classAnnotationDescriptor != null) {
            classWriter.visitAnnotation(classAnnotationDescriptor, true).visitEnd();
        }
        for (MethodSpec method : methods) {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, method.name, "()V", null, null);
            if (method.annotationDescriptor != null) {
                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(method.annotationDescriptor, true);
                if (method.annotationValue != null) {
                    annotationVisitor.visit("value", method.annotationValue);
                }
                annotationVisitor.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(0, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }
}
