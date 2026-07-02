package com.jvmguard.agent.instrument.transaction.pojo;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.instrument.Instrumenter;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.instrument.transaction.ChangeCalculator;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.DefinitionWithHandler;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.util.Logger;

import java.util.*;

public class PojoChangeCalculator extends ChangeCalculator<PojoDefinition> {

    private Map<PojoDefinition, PojoTransactionDefList> newPojoInterceptionMap;

    public PojoChangeCalculator(Map<PojoDefinition, PojoTransactionDefList> newPojoInterceptionMap, Set<PojoDefinition> oldPojoDefinitions, Instrumenter instrumenter, Map<TransactionDefinition, Map<String, Class>> calleeMap, Map<DefinitionWithHandler, Class> usedCallees) {
        super(instrumenter, calleeMap, usedCallees);
        this.newPojoInterceptionMap = newPojoInterceptionMap;
        Map<PojoDefinition, Set<DefinitionSite>> affectedClasses = getAffectedClasses(newPojoInterceptionMap);

        calculateChanges(newPojoInterceptionMap, oldPojoDefinitions, affectedClasses);

        for (PojoDefinition pojoDefinition : newPojoInterceptionMap.keySet()) {
            if (pojoDefinition.isSuperclassWithImplementingMethods()) {
                addClassWithDefiningMethods(pojoDefinition.getDeclaringClassName(), pojoDefinition);
            }
        }
    }

    private Map<PojoDefinition, Set<DefinitionSite>> getAffectedClasses(Map<PojoDefinition, PojoTransactionDefList> newPojoInterceptionMap) {
        Set<TransactionDefinition> oldCalleeMap = calleeMap.keySet();
        Set<TransactionDefinition> oldCalleeSet = new HashSet<>(Arrays.asList(oldCalleeMap.toArray(new TransactionDefinition[0])));

        final Map<PojoDefinition, Set<DefinitionSite>> affectedClasses = new HashMap<>();
        final Map<String, Set<PojoDefinition>> superClassNameToDefinitions = new HashMap<>();

        addDefinitions(newPojoInterceptionMap.keySet(), affectedClasses, superClassNameToDefinitions);
        addDefinitions(oldCalleeSet, affectedClasses, superClassNameToDefinitions);

        Logger.log(Subsystem.INSTRUMENTATION, 5, true, "searched superclasses %s\n", superClassNameToDefinitions);

        instrumenter.visitClassFileInfos(new HierarchyVisitor() {
            String baseClassName;

            @Override
            public void start(ClassFileInfo classFileInfo) {
                baseClassName = classFileInfo.getName().replace('/', '.');
                super.start(classFileInfo);
            }

            @Override
            public boolean visit(ClassFileInfo classFileInfo) {
                if (classFileInfo.isTransactionInstrumentable()) {
                    String className = classFileInfo.getName().replace('/', '.');
                    Set<PojoDefinition> definitions = superClassNameToDefinitions.get(className);
                    if (definitions != null) {
                        Logger.log(Subsystem.INSTRUMENTATION, 5, true, "found superclasses %s for base class %s\n", className, baseClassName);
                        for (PojoDefinition definition : definitions) {
                            addAffectedClass(affectedClasses, definition, baseClassName);
                        }
                    }
                }
                return true;
            }
        });
        return affectedClasses;
    }

    private static void addDefinitions(Set<? extends TransactionDefinition> definitions, Map<PojoDefinition, Set<DefinitionSite>> affectedClasses, Map<String, Set<PojoDefinition>> superClassNameToDefinitions) {
        for (TransactionDefinition transactionDefinition : definitions) {
            if (transactionDefinition instanceof PojoDefinition) {
                PojoDefinition pojoDefinition = (PojoDefinition)transactionDefinition;

                String className = pojoDefinition.getDeclaringClassName();
                if (!pojoDefinition.isInterceptSubclasses()) {
                    addAffectedClass(affectedClasses, pojoDefinition, className);
                } else {
                    Set<PojoDefinition> set = superClassNameToDefinitions.computeIfAbsent(className, k -> new HashSet<>());
                    set.add(pojoDefinition);
                }
            }
        }
    }

    private static void addAffectedClass(Map<PojoDefinition, Set<DefinitionSite>> affectedClasses, PojoDefinition pojoDefinition, String className) {
        Set<DefinitionSite> classNames = affectedClasses.computeIfAbsent(pojoDefinition, k -> new HashSet<>());
        classNames.add(new DefinitionSite(className));
    }

    @Override
    public void apply() {
        super.apply();

        Map<InterceptionMethod, List<PojoTransactionDefList>> newMethodToDefinition = new HashMap<>();
        Map<String, List<PojoTransactionDefList>> newClassNameToDefinition = new HashMap<>();

        for (PojoTransactionDefList transactionDefList : newPojoInterceptionMap.values()) {
            PojoDefinition definition = transactionDefList.getDefinition();
            String slashClassName = definition.getDeclaringClassName().replace('.', '/');
            if (definition.getMethodName() != null) {
                InterceptionMethod interceptionMethod = new InterceptionMethod(definition.getMethodName(), definition.getMethodSignature());
                if (definition.isInterceptSubclasses()) {
                    definition.setDefinedMethods(Collections.singleton(interceptionMethod));
                    List<PojoTransactionDefList> list = newClassNameToDefinition.computeIfAbsent(slashClassName, k -> new ArrayList<>());
                    list.add(transactionDefList);
                } else {
                    List<PojoTransactionDefList> list = newMethodToDefinition.computeIfAbsent(interceptionMethod, k -> new ArrayList<>());
                    list.add(transactionDefList);
                }
            } else {
                List<PojoTransactionDefList> list = newClassNameToDefinition.computeIfAbsent(slashClassName, k -> new ArrayList<>());
                list.add(transactionDefList);
            }
        }
        Logger.log(Subsystem.INSTRUMENTATION, 10, true, "pojo definitions %s %s\n", newMethodToDefinition, newClassNameToDefinition);
        instrumenter.setPojoMethodDefinitions(newMethodToDefinition);
        instrumenter.setPojoClassDefinitions(newClassNameToDefinition);
    }
}
