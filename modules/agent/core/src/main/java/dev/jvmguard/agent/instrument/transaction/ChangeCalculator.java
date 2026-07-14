package dev.jvmguard.agent.instrument.transaction;

import dev.jvmguard.agent.util.SignatureUtil;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.TargetClassGenerator;
import dev.jvmguard.agent.instrument.classInfo.DeclaredAnnotations;
import dev.jvmguard.agent.util.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

public abstract class ChangeCalculator<T extends TransactionDefinition> {

    protected Set<String> changedClassNames = new HashSet<>();
    protected Map<T, Map<String, Class>> newCalleeMap = new HashMap<>();
    protected Map<DefinitionWithHandler, Class> newUsedCallees = new HashMap<>();

    private Map<String, List<TransactionDefinition>> classesWithDefiningMethods = new HashMap<>();

    protected final Instrumenter instrumenter;
    protected final Map<TransactionDefinition, Map<String, Class>> calleeMap;
    protected final Map<DefinitionWithHandler, Class> usedCallees;

    protected ChangeCalculator(Instrumenter instrumenter, Map<TransactionDefinition, Map<String, Class>> calleeMap, Map<DefinitionWithHandler, Class> usedCallees) {
        this.instrumenter = instrumenter;
        this.calleeMap = calleeMap;
        this.usedCallees = usedCallees;
    }

    public boolean shouldRetransform(String className) {
        return changedClassNames.contains(className);
    }

    protected final void addClassWithDefiningMethods(String dottedClassName, TransactionDefinition transactionDefinition) {
        List<TransactionDefinition> list = classesWithDefiningMethods.computeIfAbsent(dottedClassName, k -> new ArrayList<>());
        list.add(transactionDefinition);
    }

    protected final void calculateChanges(Map<T, ? extends TransactionDefList> newDefinitions, Set<T> oldDefinitions, Map<T, Set<DefinitionSite>> affectedClasses) {
        Logger.log(Subsystem.INSTRUMENTATION, 20, false, "old callees: %s\n", calleeMap);

        for (Entry<T, Set<DefinitionSite>> entry : affectedClasses.entrySet()) {
            T definition = entry.getKey();
            boolean oldContains = oldDefinitions.contains(definition);
            TransactionDefList newTransactionDefList = newDefinitions.get(definition);

            if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 5)) {
                Logger.log(Subsystem.INSTRUMENTATION, 5, false, "affected: " + definition + ", " + oldContains + ", " + (newTransactionDefList != null));
            }
            if (oldContains && newTransactionDefList != null) {
                Map<String, Class> oldCallees = calleeMap.get(definition);
                if (oldCallees != null) {
                    newCalleeMap.put(definition, checkExistingDefinition(entry.getValue(), oldCallees, newTransactionDefList));
                }
            } else if (oldContains) {
                Map<String, Class> oldCallees = calleeMap.get(definition);
                if (oldCallees != null) {
                    for (DefinitionSite definitionSite : entry.getValue()) {
                        Class instrumentedCallee = oldCallees.get(definitionSite.getDefinedFor());
                        if (instrumentedCallee != null) {
                            if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 5)) {
                                Logger.log(Subsystem.INSTRUMENTATION, 5, false, "adding to changed (removed): %s\n", definitionSite);
                            }
                            changedClassNames.add(definitionSite.getDefinedFor());
                        }
                    }
                }
            } else if (newTransactionDefList != null) {
                for (DefinitionSite definitionSite : entry.getValue()) {
                    Handler handler = newTransactionDefList.getHandler(definitionSite);
                    if (handler != null) {
                        if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 5)) {
                            Logger.log(Subsystem.INSTRUMENTATION, 5, false, "adding to changed (added): %s\n", definitionSite);
                        }
                        changedClassNames.add(definitionSite.getDefinedFor());
                    }
                }
            }
        }
    }

    private Map<String, Class> checkExistingDefinition(Set<DefinitionSite> classes, Map<String, Class> oldCallees, TransactionDefList newTransactionDefList) {
        Map<String, Class> newCallees = new HashMap<>();
        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "check existing definition: %s\n", newTransactionDefList.getDefinition());

        Map<Class, TransactionDefCounter<Handler>> counterMap = new HashMap<>();

        for (DefinitionSite definitionSite : classes) {
            Class instrumentedCallee = oldCallees.get(definitionSite.getDefinedFor());
            Handler handler = newTransactionDefList.getHandler(definitionSite);

            if (instrumentedCallee != null && handler != null) {
                TransactionDefCounter<Handler> transactionDefCounter = counterMap.get(instrumentedCallee);
                if (transactionDefCounter == null) {
                    transactionDefCounter = new TransactionDefCounter<>();
                    counterMap.put(instrumentedCallee, transactionDefCounter);
                }
                transactionDefCounter.count(handler, definitionSite.getDefinedFor());

            } else if (instrumentedCallee != null || handler != null) { // only one is null
                if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 5)) {
                    Logger.log(Subsystem.INSTRUMENTATION, 5, false, "adding to changed (both): %s %s %s\n", definitionSite, instrumentedCallee != null, handler != null);
                    Logger.log(Subsystem.INSTRUMENTATION, 5, false, oldCallees);
                }
                changedClassNames.add(definitionSite.getDefinedFor());
            }
        }

        for (Entry<Class, TransactionDefCounter<Handler>> entry : counterMap.entrySet()) {
            Class instrumentedCallee = entry.getKey();
            TransactionDefCounter<Handler> transactionDefCounter = entry.getValue();

            Handler newHandler = transactionDefCounter.getBestPair();
            TargetClassGenerator.setHandler(instrumentedCallee, newHandler);
            newUsedCallees.put(new DefinitionWithHandler(newTransactionDefList.getDefinition(), newHandler), instrumentedCallee);

            transactionDefCounter.addRetransformClasses(changedClassNames);
            for (String keptClassName : transactionDefCounter.getKeptClasses()) {
                newCallees.put(keptClassName, instrumentedCallee);
            }
        }

        return newCallees;
    }

    public void apply() {
        usedCallees.putAll(newUsedCallees);
        calleeMap.putAll(newCalleeMap);
    }

    public void visitClass(Class clazz, String className) {
        addDefiningMethods(clazz, className);
    }

    private void addDefiningMethods(Class clazz, String className) {
        List<TransactionDefinition> transactionDefinitions = classesWithDefiningMethods.get(className);
        if (transactionDefinitions != null) {
            Set<InterceptionMethod> interceptionMethods = new HashSet<>();
            for (Method method : clazz.getDeclaredMethods()) {
                int modifier = method.getModifiers();
                if (Modifier.isPublic(modifier) && !Modifier.isStatic(modifier)) {
                    interceptionMethods.add(new InterceptionMethod(method.getName(), SignatureUtil.getSignature(method)));
                }
            }
            DeclaredAnnotations declaredAnnotations = instrumenter.getDeclaredAnnotations(className);
            if (declaredAnnotations != null) {
                interceptionMethods.removeAll(declaredAnnotations.getNoTransactionMethods());
            }
            for (TransactionDefinition transactionDefinition : transactionDefinitions) {
                transactionDefinition.setDefinedMethods(interceptionMethods);
            }
        }
    }


    public static class TransactionDefCounter<T extends Handler> {
        private T bestPair;

        Map<T, List<String>> transactionPairToClassNames = new HashMap<>();


        public void count(T transactionDefPair, String className) {
            List<String> list = transactionPairToClassNames.computeIfAbsent(transactionDefPair, k -> new ArrayList<>());
            list.add(className);
        }

        public T getBestPair() {
            if (bestPair == null) {
                int unchangedCount = 0;
                for (Entry<T, List<String>> entry : transactionPairToClassNames.entrySet()) {
                    if (bestPair == null || unchangedCount < entry.getValue().size()) {
                        bestPair = entry.getKey();
                        unchangedCount = entry.getValue().size();
                    }
                }
            }
            return bestPair;
        }

        public Collection<String> getKeptClasses() {
            return transactionPairToClassNames.get(getBestPair());
        }

        public void addRetransformClasses(Set<String> retransformClasses) {
            T bestPair = getBestPair();

            for (Entry<T, List<String>> entry : transactionPairToClassNames.entrySet()) {
                if (entry.getKey() != bestPair) {
                    retransformClasses.addAll(entry.getValue());
                }
            }
        }
    }

}
