package com.jvmguard.agent.instrument;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import com.jvmguard.agent.instrument.transaction.pojo.PojoTransactionDefList;

import java.util.*;

public class InstrumenterConfig {
    private Map<InterceptionMethod, List<PojoTransactionDefList>> pojoMethodDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Map<String, List<PojoTransactionDefList>> pojoClassDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Map<String, List<AnnotationTransactionDefList>> annotationDefinitions = Collections.unmodifiableMap(new HashMap<>());
    private Set<String> inheritableMethodAnnotations = Collections.unmodifiableSet(new HashSet<>());

    public synchronized Map<InterceptionMethod, List<PojoTransactionDefList>> getPojoMethodDefinitions() {
        return pojoMethodDefinitions;
    }

    public synchronized void setPojoMethodDefinitions(Map<InterceptionMethod, List<PojoTransactionDefList>> pojoMethodDefinitions) {
        this.pojoMethodDefinitions = Collections.unmodifiableMap(pojoMethodDefinitions);
    }

    public synchronized Map<String, List<PojoTransactionDefList>> getPojoClassDefinitions() {
        return pojoClassDefinitions;
    }

    public synchronized void setPojoClassDefinitions(Map<String, List<PojoTransactionDefList>> pojoClassDefinitions) {
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
