package dev.jvmguard.agent.instrument.transaction.matched;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.instrument.transaction.ChangeCalculator;
import dev.jvmguard.agent.instrument.transaction.DefinitionSite;
import dev.jvmguard.agent.instrument.transaction.DefinitionWithHandler;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;
import dev.jvmguard.agent.util.Logger;

import java.util.*;

public class MatchedChangeCalculator extends ChangeCalculator<MatchedDefinition> {

    private Map<MatchedDefinition, MatchedTransactionDefList> newPojoInterceptionMap;

    public MatchedChangeCalculator(Map<MatchedDefinition, MatchedTransactionDefList> newPojoInterceptionMap, Set<MatchedDefinition> oldMatchedDefinitions, Instrumenter instrumenter, Map<TransactionDefinition, Map<String, Class>> calleeMap, Map<DefinitionWithHandler, Class> usedCallees) {
        super(instrumenter, calleeMap, usedCallees);
        this.newPojoInterceptionMap = newPojoInterceptionMap;
        Map<MatchedDefinition, Set<DefinitionSite>> affectedClasses = getAffectedClasses(newPojoInterceptionMap);

        calculateChanges(newPojoInterceptionMap, oldMatchedDefinitions, affectedClasses);

        for (MatchedDefinition pojoDefinition : newPojoInterceptionMap.keySet()) {
            if (pojoDefinition.isSuperclassWithImplementingMethods()) {
                addClassWithDefiningMethods(pojoDefinition.getDeclaringClassName(), pojoDefinition);
            }
        }
    }

    private Map<MatchedDefinition, Set<DefinitionSite>> getAffectedClasses(Map<MatchedDefinition, MatchedTransactionDefList> newPojoInterceptionMap) {
        Set<TransactionDefinition> oldCalleeMap = calleeMap.keySet();
        Set<TransactionDefinition> oldCalleeSet = new HashSet<>(Arrays.asList(oldCalleeMap.toArray(new TransactionDefinition[0])));

        final Map<MatchedDefinition, Set<DefinitionSite>> affectedClasses = new HashMap<>();
        final Map<String, Set<MatchedDefinition>> superClassNameToDefinitions = new HashMap<>();

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
                    Set<MatchedDefinition> definitions = superClassNameToDefinitions.get(className);
                    if (definitions != null) {
                        Logger.log(Subsystem.INSTRUMENTATION, 5, true, "found superclasses %s for base class %s\n", className, baseClassName);
                        for (MatchedDefinition definition : definitions) {
                            addAffectedClass(affectedClasses, definition, baseClassName);
                        }
                    }
                }
                return true;
            }
        });
        return affectedClasses;
    }

    private static void addDefinitions(Set<? extends TransactionDefinition> definitions, Map<MatchedDefinition, Set<DefinitionSite>> affectedClasses, Map<String, Set<MatchedDefinition>> superClassNameToDefinitions) {
        for (TransactionDefinition transactionDefinition : definitions) {
            if (transactionDefinition instanceof MatchedDefinition) {
                MatchedDefinition pojoDefinition = (MatchedDefinition)transactionDefinition;

                String className = pojoDefinition.getDeclaringClassName();
                if (!pojoDefinition.isInterceptSubclasses()) {
                    addAffectedClass(affectedClasses, pojoDefinition, className);
                } else {
                    Set<MatchedDefinition> set = superClassNameToDefinitions.computeIfAbsent(className, k -> new HashSet<>());
                    set.add(pojoDefinition);
                }
            }
        }
    }

    private static void addAffectedClass(Map<MatchedDefinition, Set<DefinitionSite>> affectedClasses, MatchedDefinition pojoDefinition, String className) {
        Set<DefinitionSite> classNames = affectedClasses.computeIfAbsent(pojoDefinition, k -> new HashSet<>());
        classNames.add(new DefinitionSite(className));
    }

    @Override
    public void apply() {
        super.apply();

        Map<InterceptionMethod, List<MatchedTransactionDefList>> newMethodToDefinition = new HashMap<>();
        Map<String, List<MatchedTransactionDefList>> newClassNameToDefinition = new HashMap<>();

        for (MatchedTransactionDefList transactionDefList : newPojoInterceptionMap.values()) {
            MatchedDefinition definition = transactionDefList.getDefinition();
            String slashClassName = definition.getDeclaringClassName().replace('.', '/');
            if (definition.getMethodName() != null) {
                InterceptionMethod interceptionMethod = new InterceptionMethod(definition.getMethodName(), definition.getMethodSignature());
                if (definition.isInterceptSubclasses()) {
                    definition.setDefinedMethods(Collections.singleton(interceptionMethod));
                    List<MatchedTransactionDefList> list = newClassNameToDefinition.computeIfAbsent(slashClassName, k -> new ArrayList<>());
                    list.add(transactionDefList);
                } else {
                    List<MatchedTransactionDefList> list = newMethodToDefinition.computeIfAbsent(interceptionMethod, k -> new ArrayList<>());
                    list.add(transactionDefList);
                }
            } else {
                List<MatchedTransactionDefList> list = newClassNameToDefinition.computeIfAbsent(slashClassName, k -> new ArrayList<>());
                list.add(transactionDefList);
            }
        }
        Logger.log(Subsystem.INSTRUMENTATION, 10, true, "pojo definitions %s %s\n", newMethodToDefinition, newClassNameToDefinition);
        instrumenter.setPojoMethodDefinitions(newMethodToDefinition);
        instrumenter.setPojoClassDefinitions(newClassNameToDefinition);
    }
}
