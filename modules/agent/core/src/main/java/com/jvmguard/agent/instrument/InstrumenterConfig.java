package com.jvmguard.agent.instrument;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import com.jvmguard.agent.instrument.transaction.matched.MatchedTransactionDefList;

import java.util.*;

public class InstrumenterConfig {
    private Map<InterceptionMethod, List<MatchedTransactionDefList>> pojoMethodDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Map<String, List<MatchedTransactionDefList>> pojoClassDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Map<String, List<AnnotationTransactionDefList>> annotationDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Set<String> inheritableMethodAnnotations = Collections.unmodifiableSet(new HashSet<>());

    public synchronized Map<InterceptionMethod, List<MatchedTransactionDefList>> getPojoMethodDefinitions() {
        return pojoMethodDefinitions;
    }

    public synchronized void setPojoMethodDefinitions(Map<InterceptionMethod, List<MatchedTransactionDefList>> pojoMethodDefinitions) {
        this.pojoMethodDefinitions = Collections.unmodifiableMap(pojoMethodDefinitions);
    }

    public synchronized Map<String, List<MatchedTransactionDefList>> getPojoClassDefinitions() {
        return pojoClassDefinitions;
    }

    public synchronized void setPojoClassDefinitions(Map<String, List<MatchedTransactionDefList>> pojoClassDefinitions) {
        this.pojoClassDefinitions = Collections.unmodifiableMap(pojoClassDefinitions);
    }

    public synchronized Map<String, List<AnnotationTransactionDefList>> getAnnotationDefinitions() {
        return annotationDefinitions;
    }

    public synchronized void setAnnotationDefinitions(Map<String, List<AnnotationTransactionDefList>> annotationDefinitions, Set<String> inheritableMethodAnnotations) {
        this.annotationDefinitions = Collections.unmodifiableMap(annotationDefinitions);
        this.inheritableMethodAnnotations = Collections.unmodifiableSet(inheritableMethodAnnotations);
    }

    public synchronized Set<String> getInheritableMethodAnnotations() {
        return inheritableMethodAnnotations;
    }

    public synchronized void freeMemory() {
        pojoMethodDefinitions = Collections.unmodifiableMap(new HashMap<>());
        pojoClassDefinitions = Collections.unmodifiableMap(new HashMap<>());
        annotationDefinitions = Collections.unmodifiableMap(new HashMap<>());
        inheritableMethodAnnotations = Collections.unmodifiableSet(new HashSet<>());
    }
}
