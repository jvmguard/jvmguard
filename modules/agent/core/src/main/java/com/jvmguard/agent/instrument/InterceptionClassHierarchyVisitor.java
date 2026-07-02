package com.jvmguard.agent.instrument;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.callee.AnnotationHandler;
import com.jvmguard.agent.callee.PojoHandler;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import com.jvmguard.agent.instrument.classInfo.DevOpsAnnotationInfo;
import com.jvmguard.agent.instrument.classInfo.DevOpsAnnotations;
import com.jvmguard.agent.instrument.interceptions.AnnotationInterception;
import com.jvmguard.agent.instrument.interceptions.BaseInterception;
import com.jvmguard.agent.instrument.interceptions.DevOpsClassInterception;
import com.jvmguard.agent.instrument.interceptions.TransactionInterception;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.DefinitionSite.AnnotationDefinitionSite;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import com.jvmguard.agent.instrument.transaction.annotation.CustomAnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DevOpsAnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.pojo.PojoDefinition;
import com.jvmguard.agent.instrument.transaction.pojo.PojoTransactionDefList;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.annotation.Inheritance.Mode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterceptionClassHierarchyVisitor extends HierarchyVisitor {
    private final DevOpsAnnotations devOpsAnnotations;
    private final Instrumenter instrumenter;
    private boolean fullyDefined = true;
    private Map<String, List<PojoTransactionDefList>> pojoDefinitions;
    private Map<String, List<AnnotationTransactionDefList>> annotationDefinitions;
    private Set<BaseInterception> classInterceptions;

    private String dottedBaseClassName;

    private DefinitionSite definitionSite = new DefinitionSite();
    private AnnotationDefinitionSite annotationDefinitionSite = new AnnotationDefinitionSite();

    public InterceptionClassHierarchyVisitor(Map<String, List<PojoTransactionDefList>> pojoDefinitions, Map<String, List<AnnotationTransactionDefList>> annotationDefinitions, Set<BaseInterception> classInterceptions, DevOpsAnnotations devOpsAnnotations, Instrumenter instrumenter) {
        this.pojoDefinitions = pojoDefinitions;
        this.annotationDefinitions = annotationDefinitions;
        this.classInterceptions = classInterceptions;
        this.devOpsAnnotations = devOpsAnnotations;
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
        Set<InterceptionMethod> noTransactionMethods = devOpsAnnotations == null ? null : devOpsAnnotations.getNoTransactionMethods();

        if (classFileInfo.getClassAnnotations() != null) {
            for (Object storedAnnotation : classFileInfo.getClassAnnotations()) {
                if (storedAnnotation instanceof String) {
                    checkStoredString(classFileInfo, dottedDeclaringClassName, isBaseClass, false, noTransactionMethods, (String)storedAnnotation);
                } else if (storedAnnotation instanceof DevOpsAnnotationInfo) {
                    checkDevOpsTransactionInfo(dottedDeclaringClassName, noTransactionMethods, (DevOpsAnnotationInfo)storedAnnotation);
                }
            }
        }
        if (classFileInfo.getMethodAnnotations() != null) {
            for (Object storedAnnotation : classFileInfo.getMethodAnnotations()) {
                if (storedAnnotation instanceof String) {
                    checkStoredString(classFileInfo, dottedDeclaringClassName, isBaseClass, true, noTransactionMethods, (String)storedAnnotation);
                } else if (storedAnnotation instanceof DevOpsAnnotationInfo) {
                    checkDevOpsTransactionInfo(dottedDeclaringClassName, noTransactionMethods, (DevOpsAnnotationInfo)storedAnnotation);
                }
            }
        }

        List<PojoTransactionDefList> pojoTransactionDefLists = pojoDefinitions.get(classFileInfo.getName());
        if (pojoTransactionDefLists != null) {
            Logger.log(Subsystem.INSTRUMENTATION, 6, false, "pojo defs for %s: %s\n", classFileInfo.getName(), pojoTransactionDefLists);
            for (PojoTransactionDefList transactionDefList : pojoTransactionDefLists) {
                PojoDefinition definition = transactionDefList.getDefinition();
                if (definition.isInterceptSubclasses() || isBaseClass) {
                    if (definition.isSuperclassWithImplementingMethods() && definition.getDefinedMethods() == null) {
                        Logger.log(Subsystem.INSTRUMENTATION, 6, false, "did not find methods for %s: %s\n", classFileInfo.getName(), definition);
                        continue;
                    }
                    PojoHandler pojoHandler = transactionDefList.getHandler(definitionSite.init(dottedBaseClassName));
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
            String matchAllName = DevOpsAnnotationDefinition.getMatchAllDescriptor(annotationDescriptor);
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
                        if (definition instanceof DevOpsAnnotationDefinition && devOpsAnnotations != null) {
                            // this will only happen for isBaseClass, otherwise isApplicable returns false because stored strings are only used for non-inheritable devops transactions
                            classInterceptions.add(new DevOpsClassInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName, DevOpsAnnotationInfo.create(devOpsAnnotations.getClassTransaction(), classFileInfo.getName()), null));
                        } else if (definition instanceof CustomAnnotationDefinition) {
                            CustomAnnotationDefinition customAnnotationDefinition = (CustomAnnotationDefinition)definition;
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

    private void checkDevOpsTransactionInfo(String dottedDeclaringClassName, Set<InterceptionMethod> noTransactionMethods, DevOpsAnnotationInfo devOpsAnnotationInfo) {
        if (devOpsAnnotationInfo.matches(dottedBaseClassName)) {
            String annotationDescriptor = devOpsAnnotationInfo.getAnnotationDescriptor();
            List<AnnotationTransactionDefList> interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
            if (interceptionAnnotations == null) {
                String matchAllName = DevOpsAnnotationDefinition.getMatchAllDescriptor(annotationDescriptor);
                if (matchAllName != null && !matchAllName.equals(annotationDescriptor)) {
                    annotationDescriptor = matchAllName;
                    interceptionAnnotations = annotationDefinitions.get(annotationDescriptor);
                }
            }

            if (interceptionAnnotations != null) {
                for (AnnotationTransactionDefList transactionDefList : interceptionAnnotations) {
                    AnnotationDefinition definition = transactionDefList.getDefinition();
                    AnnotationHandler annotationHandler = transactionDefList.getHandler(annotationDefinitionSite.init(dottedBaseClassName, devOpsAnnotationInfo.getInheritance().value() == Mode.WITH_SUBCLASS_NAMES ? dottedBaseClassName : dottedDeclaringClassName));
                    if (annotationHandler != null) {
                        classInterceptions.add(new DevOpsClassInterception(definition, noTransactionMethods, annotationHandler, dottedDeclaringClassName, devOpsAnnotationInfo, devOpsAnnotationInfo.getDefinedMethods()));
                    }
                }
            }
        } else if (Logger.isEnabled(Subsystem.USER, 1)) {
            Logger.log(Subsystem.USER, 1, true, "class %s was rejected by inheritance filter '%s' of DevOps annotation declared on class %s\n", dottedBaseClassName, devOpsAnnotationInfo.getInheritance().filter(), dottedDeclaringClassName);
        }
    }
}
