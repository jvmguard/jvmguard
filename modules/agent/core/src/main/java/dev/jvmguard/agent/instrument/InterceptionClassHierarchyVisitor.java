package dev.jvmguard.agent.instrument;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.callee.AnnotationHandler;
import dev.jvmguard.agent.callee.MatchedHandler;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import dev.jvmguard.agent.instrument.classInfo.DeclaredAnnotationInfo;
import dev.jvmguard.agent.instrument.classInfo.DeclaredAnnotations;
import dev.jvmguard.agent.instrument.interceptions.AnnotationInterception;
import dev.jvmguard.agent.instrument.interceptions.BaseInterception;
import dev.jvmguard.agent.instrument.interceptions.DeclaredClassInterception;
import dev.jvmguard.agent.instrument.interceptions.TransactionInterception;
import dev.jvmguard.agent.instrument.transaction.DefinitionSite;
import dev.jvmguard.agent.instrument.transaction.DefinitionSite.AnnotationDefinitionSite;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import dev.jvmguard.agent.instrument.transaction.annotation.MappedAnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.DeclaredAnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedTransactionDefList;
import dev.jvmguard.agent.util.Logger;
import dev.jvmguard.annotation.Inheritance.Mode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterceptionClassHierarchyVisitor extends HierarchyVisitor {
    private final DeclaredAnnotations declaredAnnotations;
    private final Instrumenter instrumenter;
    private boolean fullyDefined = true;
    private Map<String, List<MatchedTransactionDefList>> pojoDefinitions;
    private Map<String, List<AnnotationTransactionDefList>> annotationDefinitions;
    private Set<BaseInterception> classInterceptions;

    private String dottedBaseClassName;

    private DefinitionSite definitionSite = new DefinitionSite();
    private AnnotationDefinitionSite annotationDefinitionSite = new AnnotationDefinitionSite();

    public InterceptionClassHierarchyVisitor(Map<String, List<MatchedTransactionDefList>> pojoDefinitions, Map<String, List<AnnotationTransactionDefList>> annotationDefinitions, Set<BaseInterception> classInterceptions, DeclaredAnnotations declaredAnnotations, Instrumenter instrumenter) {
        this.pojoDefinitions = pojoDefinitions;
        this.annotationDefinitions = annotationDefinitions;
        this.classInterceptions = classInterceptions;
        this.declaredAnnotations = declaredAnnotations;
        this.instrumenter = instrumenter;
    }

    public boolean isFullyDefined() {
        return fullyDefined;
    }

    @Override
    public boolean visit(ClassFileInfo classFileInfo) {
        String dottedDeclaringClassName = classFileInfo.getName().replace('/', '.');

        boolean isBaseClass = classFileInfo == baseClassFileInfo;
        if (isBaseClass) {
            dottedBaseClassName = classFileInfo.getName().replace('/', '.');
        }
        if (!classFileInfo.isDefined()) {
            fullyDefined = false;
        }
        Set<InterceptionMethod> noTransactionMethods = declaredAnnotations == null ? null : declaredAnnotations.getNoTransactionMethods();

        if (classFileInfo.getClassAnnotations() != null) {
            for (Object storedAnnotation : classFileInfo.getClassAnnotations()) {
                if (storedAnnotation instanceof String) {
                    checkStoredString(classFileInfo, dottedDeclaringClassName, isBaseClass, false, noTransactionMethods, (String)storedAnnotation);
                } else if (storedAnnotation instanceof DeclaredAnnotationInfo) {
                    checkDeclaredTransactionInfo(dottedDeclaringClassName, noTransactionMethods, (DeclaredAnnotationInfo)storedAnnotation);
                }
            }
        }
        if (classFileInfo.getMethodAnnotations() != null) {
            for (Object storedAnnotation : classFileInfo.getMethodAnnotations()) {
                if (storedAnnotation instanceof String) {
                    checkStoredString(classFileInfo, dottedDeclaringClassName, isBaseClass, true, noTransactionMethods, (String)storedAnnotation);
                } else if (storedAnnotation instanceof DeclaredAnnotationInfo) {
                    checkDeclaredTransactionInfo(dottedDeclaringClassName, noTransactionMethods, (DeclaredAnnotationInfo)storedAnnotation);
                }
            }
        }

        List<MatchedTransactionDefList> pojoTransactionDefLists = pojoDefinitions.get(classFileInfo.getName());
        if (pojoTransactionDefLists != null) {
            Logger.log(Subsystem.INSTRUMENTATION, 6, false, "pojo defs for %s: %s\n", classFileInfo.getName(), pojoTransactionDefLists);
            for (MatchedTransactionDefList transactionDefList : pojoTransactionDefLists) {
                MatchedDefinition definition = transactionDefList.getDefinition();
                if (definition.isInterceptSubclasses() || isBaseClass) {
                    if (definition.isSuperclassWithImplementingMethods() && definition.getDefinedMethods() == null) {
                        Logger.log(Subsystem.INSTRUMENTATION, 6, false, "did not find methods for %s: %s\n", classFileInfo.getName(), definition);
                        continue;
                    }
                    MatchedHandler pojoHandler = transactionDefList.getHandler(definitionSite.init(dottedBaseClassName));
                    if (pojoHandler != null) {
                        classInterceptions.add(new TransactionInterception(definition, noTransactionMethods, pojoHandler));
                    }
                }
            }
        }
        return true;
    }

    private void checkStoredString(ClassFileInfo classFileInfo, String dottedDeclaringClassName, boolean isBaseClass, boolean methodAnnotation, Set<InterceptionMethod> noTransactionMethods, String annotationDescriptor) {
        List<AnnotationTransactionDefList> interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
        if (interceptionAnnotations == null) {
            String matchAllName = DeclaredAnnotationDefinition.getMatchAllDescriptor(annotationDescriptor);
            if (matchAllName != null && !matchAllName.equals(annotationDescriptor)) {
                annotationDescriptor = matchAllName;
                interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
            }
        }
        if (interceptionAnnotations != null) {
            for (AnnotationTransactionDefList transactionDefList : interceptionAnnotations) {
                AnnotationDefinition definition = transactionDefList.getDefinition();
                if (isApplicable(definition, isBaseClass, methodAnnotation)) {
                    AnnotationHandler annotationHandler = transactionDefList.getHandler(annotationDefinitionSite.init(dottedBaseClassName, dottedDeclaringClassName));
                    if (annotationHandler != null) {
                        if (definition instanceof DeclaredAnnotationDefinition && declaredAnnotations != null) {
                            // this will only happen for isBaseClass, otherwise isApplicable returns false because stored strings are only used for non-inheritable declared transactions
                            classInterceptions.add(new DeclaredClassInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName, DeclaredAnnotationInfo.create(declaredAnnotations.getClassTransaction(), classFileInfo.getName()), null));
                        } else if (definition instanceof MappedAnnotationDefinition) {
                            MappedAnnotationDefinition customAnnotationDefinition = (MappedAnnotationDefinition)definition;
                            if (customAnnotationDefinition.isClassWithImplementingOnly() && definition.getDefinedMethods() == null) {
                                continue;
                            }
                            if (customAnnotationDefinition.isMethodAnnotation() && customAnnotationDefinition.isInheritable()) {
                                if (definition.getDefinedMethods() == null) {
                                    Set<InterceptionMethod> methods = instrumenter.getMethodAnnotations(dottedDeclaringClassName, annotationDescriptor);
                                    if (methods == null) {
                                        continue;
                                    }
                                    definition.setDefinedMethods(methods);
                                }
                            }
                            classInterceptions.add(new AnnotationInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName));
                        } else {
                            classInterceptions.add(new AnnotationInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName));
                        }
                    }
                }
            }
        }
    }

    private boolean isApplicable(AnnotationDefinition definition, boolean isBaseClass, boolean methodAnnotation) {
        if (definition.isMethodAnnotation()) {
            return methodAnnotation && definition.isInheritable();
        } else {
            return !methodAnnotation && (definition.isInheritable() || isBaseClass);
        }
    }

    private void checkDeclaredTransactionInfo(String dottedDeclaringClassName, Set<InterceptionMethod> noTransactionMethods, DeclaredAnnotationInfo declaredAnnotationInfo) {
        if (declaredAnnotationInfo.matches(dottedBaseClassName)) {
            String annotationDescriptor = declaredAnnotationInfo.getAnnotationDescriptor();
            List<AnnotationTransactionDefList> interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
            if (interceptionAnnotations == null) {
                String matchAllName = DeclaredAnnotationDefinition.getMatchAllDescriptor(annotationDescriptor);
                if (matchAllName != null && !matchAllName.equals(annotationDescriptor)) {
                    annotationDescriptor = matchAllName;
                    interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
                }
            }

            if (interceptionAnnotations != null) {
                for (AnnotationTransactionDefList transactionDefList : interceptionAnnotations) {
                    AnnotationDefinition definition = transactionDefList.getDefinition();
                    AnnotationHandler annotationHandler = transactionDefList.getHandler(annotationDefinitionSite.init(dottedBaseClassName, declaredAnnotationInfo.getInheritance().value() == Mode.WITH_SUBCLASS_NAMES ? dottedBaseClassName : dottedDeclaringClassName));
                    if (annotationHandler != null) {
                        classInterceptions.add(new DeclaredClassInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName, declaredAnnotationInfo, declaredAnnotationInfo.getDefinedMethods()));
                    }
                }
            }
        } else if (Logger.isEnabled(Subsystem.USER, 1)) {
            Logger.log(Subsystem.USER, 1, true, "class %s was rejected by inheritance filter '%s' of Declared annotation declared on class %s\n", dottedBaseClassName, declaredAnnotationInfo.getInheritance().filter(), dottedDeclaringClassName);
        }
    }
}
