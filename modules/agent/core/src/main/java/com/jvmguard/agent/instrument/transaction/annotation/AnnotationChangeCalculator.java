package com.jvmguard.agent.instrument.transaction.annotation;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.instrument.Instrumenter;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import com.jvmguard.agent.instrument.classInfo.DeclaredAnnotationInfo;
import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.instrument.transaction.ChangeCalculator;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.DefinitionSite.AnnotationDefinitionSite;
import com.jvmguard.agent.instrument.transaction.DefinitionWithHandler;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition.SearchType;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.annotation.Inheritance.Mode;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class AnnotationChangeCalculator extends ChangeCalculator<AnnotationDefinition> {
    private Map<AnnotationDefinition, AnnotationTransactionDefList> newAnnotationDefs;
    private Set<String> newInheritedMethodAnnotations = new HashSet<>();

    private Map<String, Set<String>> dottedClassesWithMissingAnnotationMethods = new HashMap<>();


    public AnnotationChangeCalculator(Map<AnnotationDefinition, AnnotationTransactionDefList> newAnnotationDefs, Set<AnnotationDefinition> oldAnnotationDefs, Instrumenter instrumenter, Map<TransactionDefinition, Map<String, Class>> calleeMap, Map<DefinitionWithHandler, Class> usedCallees) {
        super(instrumenter, calleeMap, usedCallees);
        this.newAnnotationDefs = newAnnotationDefs;
        for (AnnotationDefinition annotationDefinition : newAnnotationDefs.keySet()) {
            if (annotationDefinition.isMethodAnnotation() && annotationDefinition.isInheritable()) {
                newInheritedMethodAnnotations.add(annotationDefinition.getName());
            }
        }
        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "new inherited methods: %s\n", newInheritedMethodAnnotations);

        Map<AnnotationDefinition, Set<DefinitionSite>> affectedClasses = getAffectedClasses(newAnnotationDefs, oldAnnotationDefs, instrumenter);
        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "annotation affected classes: %s\n", affectedClasses);
        calculateChanges(newAnnotationDefs, oldAnnotationDefs, affectedClasses);
    }

    private Map<AnnotationDefinition, Set<DefinitionSite>> getAffectedClasses(Map<AnnotationDefinition, AnnotationTransactionDefList> newAnnotationDefs, Set<AnnotationDefinition> oldAnnotationDefs, final Instrumenter instrumenter) {
        final Map<AnnotationDefinition, Set<DefinitionSite>> affectedClasses = new HashMap<>();

        final Map<String, Set<AnnotationDefinition>> searchedMethodAnnotations = new HashMap<>();
        addUsedDefinitions(newAnnotationDefs.keySet(), searchedMethodAnnotations, SearchType.METHOD);
        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "method new: %s %s\n", searchedMethodAnnotations, newAnnotationDefs.keySet());
        addUsedDefinitions(oldAnnotationDefs, searchedMethodAnnotations, SearchType.METHOD);

        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "method: %s\n", searchedMethodAnnotations);

        final Map<String, Set<AnnotationDefinition>> searchedInheritedMethodAnnotations = new HashMap<>();
        addUsedDefinitions(newAnnotationDefs.keySet(), searchedInheritedMethodAnnotations, SearchType.INHERITED_METHOD);
        addUsedDefinitions(oldAnnotationDefs, searchedInheritedMethodAnnotations, SearchType.INHERITED_METHOD);

        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "inherited method: %s\n", searchedInheritedMethodAnnotations);

        final Map<String, Set<AnnotationDefinition>> searchedClassAnnotations = new HashMap<>();
        addUsedDefinitions(newAnnotationDefs.keySet(), searchedClassAnnotations, SearchType.CLASS);
        addUsedDefinitions(oldAnnotationDefs, searchedClassAnnotations, SearchType.CLASS);

        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "class: %s\n", searchedClassAnnotations);

        final Map<String, Set<AnnotationDefinition>> searchedInheritedClassAnnotations = new HashMap<>();
        addUsedDefinitions(newAnnotationDefs.keySet(), searchedInheritedClassAnnotations, SearchType.INHERITED_CLASS);
        addUsedDefinitions(oldAnnotationDefs, searchedInheritedClassAnnotations, SearchType.INHERITED_CLASS);

        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "inherited class: %s\n", searchedInheritedClassAnnotations);

        instrumenter.visitClassFileInfos(new HierarchyVisitor() {
            String baseClassName;

            @Override
            public boolean visit(ClassFileInfo classFileInfo) {
                String className = classFileInfo.getName().replace('/', '.');

                if (classFileInfo == baseClassFileInfo) {
                    baseClassName = className;

                    checkAnnotations(className, classFileInfo.getMethodAnnotations(), searchedMethodAnnotations, false);
                    checkAnnotations(className, classFileInfo.getClassAnnotations(), searchedClassAnnotations, false);

                    if (!newInheritedMethodAnnotations.isEmpty()) {
                        checkMissingInheritedMethodAnnotations(className, classFileInfo.getMethodAnnotations());
                    }

                }
                checkAnnotations(className, classFileInfo.getMethodAnnotations(), searchedInheritedMethodAnnotations, true);
                checkAnnotations(className, classFileInfo.getClassAnnotations(), searchedInheritedClassAnnotations, true);
                return !searchedInheritedClassAnnotations.isEmpty() || !searchedInheritedMethodAnnotations.isEmpty();
            }

            private void checkAnnotations(String className, Object[] definedAnnotations, Map<String, Set<AnnotationDefinition>> searchedAnnotations, boolean inheritable) {
                if (searchedAnnotations != null && definedAnnotations != null) {
                    if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 20)) {
                        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "stored annotations %s %s (%s): %s\n", baseClassName, className, inheritable, Arrays.toString(definedAnnotations));
                    }
                    for (Object storedAnnotation : definedAnnotations) {
                        if (storedAnnotation instanceof String) {
                            String storedAnnotationName = (String)storedAnnotation;
                            if (!inheritable || !DeclaredAnnotationDefinition.isDeclaredDescriptor(storedAnnotationName)) {
                                checkAffectedClasses(searchedAnnotations, storedAnnotationName, className);
                            }
                        } else if (storedAnnotation instanceof DeclaredAnnotationInfo) { // inheritable
                            if (inheritable) {
                                DeclaredAnnotationInfo declaredAnnotationInfo = (DeclaredAnnotationInfo)storedAnnotation;
                                if (declaredAnnotationInfo.matches(baseClassName)) {
                                    Logger.log(Subsystem.INSTRUMENTATION, 20, false, "checking declared for %s, triggered by %s: %s\n", baseClassName, className, declaredAnnotationInfo.getAnnotationDescriptor());
                                    checkAffectedClasses(searchedAnnotations, declaredAnnotationInfo.getAnnotationDescriptor(),
                                        declaredAnnotationInfo.getInheritance().value() == Mode.WITH_SUBCLASS_NAMES ? baseClassName : className);
                                }
                            }
                        } else {
                            Logger.log(Subsystem.INSTRUMENTATION, 0, false, "unknown stored annotation type: %s (%s): %s\n", className, storedAnnotation == null ? null : storedAnnotation.getClass().getName(), storedAnnotation);
                        }
                    }
                }
            }

            private void checkAffectedClasses(Map<String, Set<AnnotationDefinition>> searchedAnnotations, String annotationDescriptor, String definedOn) {
                Set<AnnotationDefinition> definitions = searchedAnnotations.get(annotationDescriptor);
                if (definitions == null) {
                    String matchAllName = DeclaredAnnotationDefinition.getMatchAllDescriptor(annotationDescriptor);
                    if (matchAllName != null && !matchAllName.equals(annotationDescriptor)) {
                        annotationDescriptor = matchAllName;
                        definitions = searchedAnnotations.get(annotationDescriptor);
                    }
                }

                if (definitions != null) {
                    for (AnnotationDefinition definition : definitions) {
                        Set<DefinitionSite> classNames = affectedClasses.computeIfAbsent(definition, k -> new HashSet<>());
                        if (definition instanceof MappedAnnotationDefinition) {
                            MappedAnnotationDefinition customAnnotationDefinition = (MappedAnnotationDefinition)definition;
                            if (customAnnotationDefinition.isClassWithImplementingOnly()) {
                                addClassWithDefiningMethods(definedOn.replace('/', '.'), definition);
                            }
                        }
                        classNames.add(new AnnotationDefinitionSite(baseClassName, definedOn));
                    }
                }
            }
        });
        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "missing annotation methods: %s\n", dottedClassesWithMissingAnnotationMethods);

        return affectedClasses;
    }

    private void checkMissingInheritedMethodAnnotations(String className, Object[] methodAnnotations) {
        if (methodAnnotations != null) {
            for (Object storedAnnotation : methodAnnotations) {
                if (storedAnnotation instanceof String) {
                    String storedAnnotationName = (String)storedAnnotation;
                    if (newInheritedMethodAnnotations.contains(storedAnnotationName)) {
                        if (instrumenter.getMethodAnnotations(className, storedAnnotationName) == null) {
                            Logger.log(Subsystem.INSTRUMENTATION, 10, false, "missed: %s %s\n", className, storedAnnotationName);
                            String dottedClassName = className.replace('/', '.');
                            Set<String> annotationNames = dottedClassesWithMissingAnnotationMethods.computeIfAbsent(dottedClassName, k -> new HashSet<>());
                            annotationNames.add(storedAnnotationName);

                        }
                    }
                }
            }
        }
    }

    private void addUsedDefinitions(Collection<AnnotationDefinition> definitions, Map<String, Set<AnnotationDefinition>> searchedAnnotations, SearchType searchType) {
        for (AnnotationDefinition definition : definitions) {
            String searchName = definition.getUsedAnnotationDescriptor(searchType);
            if (searchName != null) {
                Set<AnnotationDefinition> set = searchedAnnotations.computeIfAbsent(searchName, k -> new HashSet<>());
                set.add(definition);
            }
        }
    }

    @Override
    public void visitClass(Class clazz, String className) {
        super.visitClass(clazz, className);
        if (!dottedClassesWithMissingAnnotationMethods.isEmpty()) {
            Set<String> missingAnnotations = dottedClassesWithMissingAnnotationMethods.get(className);
            if (missingAnnotations != null) {
                Map<String, Set<InterceptionMethod>> annotationDescriptorToMethods = new HashMap<>();
                try {
                    for (Method method : clazz.getDeclaredMethods()) {
                        for (Annotation annotation : method.getDeclaredAnnotations()) {
                            String annotationDescriptor = Type.getDescriptor(annotation.annotationType());
                            if (missingAnnotations.contains(annotationDescriptor)) {
                                Set<InterceptionMethod> methods = annotationDescriptorToMethods.computeIfAbsent(annotationDescriptor, k -> new HashSet<>());
                                methods.add(new InterceptionMethod(method.getName(), Type.getMethodDescriptor(method)));
                            }
                        }
                    }
                    Logger.log(Subsystem.INSTRUMENTATION, 10, true, "adding %s: %s\n", className, annotationDescriptorToMethods);
                    for (Entry<String, Set<InterceptionMethod>> entry : annotationDescriptorToMethods.entrySet()) {
                        instrumenter.setMethodAnnotations(className, entry.getKey(), entry.getValue());

                    }
                } catch (Throwable t) {
                    Logger.log(Subsystem.INSTRUMENTATION, 0, true, t);
                }
            }
        }
    }


    @Override
    public void apply() {
        super.apply();

        Map<String, List<AnnotationTransactionDefList>> annotationNameToDefinition = new HashMap<>();

        for (AnnotationTransactionDefList transactionDefList : newAnnotationDefs.values()) {
            List<AnnotationTransactionDefList> list = annotationNameToDefinition.computeIfAbsent(transactionDefList.getDefinition().getName(), k -> new ArrayList<>());
            list.add(transactionDefList);
        }


        instrumenter.setAnnotationDefinitions(annotationNameToDefinition, newInheritedMethodAnnotations);
    }
}
