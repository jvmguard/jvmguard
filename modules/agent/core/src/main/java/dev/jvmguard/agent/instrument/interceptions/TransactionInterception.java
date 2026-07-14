package dev.jvmguard.agent.instrument.interceptions;

import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;

import java.util.Set;

public class TransactionInterception extends BaseInterception {
    private final TransactionDefinition definition;
    private final Set<InterceptionMethod> noTransactionMethods;
    private final Handler handler;

    public TransactionInterception(TransactionDefinition definition, Handler handler) { // should be called for concrete method interceptions only
        this(definition, null, handler);
    }

    public TransactionInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler) {
        this.definition = definition;
        this.handler = handler;
        this.noTransactionMethods = noTransactionMethods;
    }

    public String getUsedClassName(String instrumentedClassName) {
        return instrumentedClassName;
    }

    @Override
    public String getDeclaringClassName() {
        return definition.getDeclaringClassName();
    }

    @Override
    public boolean isExcluded(InterceptionMethod method) {
        return noTransactionMethods != null && noTransactionMethods.contains(method);
    }

    @Override
    public Set<InterceptionMethod> getDefinedMethods() {
        return definition.getDefinedMethods();
    }

    @Override
    public boolean isProtectedAndPackageMethods() {
        return definition.isProtectedAndPackageMethods();
    }

    @Override
    public boolean isEnter() {
        return true;
    }

    @Override
    public boolean isExit() {
        return true;
    }

    @Override
    public boolean isException() {
        return true;
    }

    @Override
    public boolean isStaticMethods() {
        return definition.isStaticMethods();
    }

    @Override
    public boolean isThisForExit() {
        return false;
    }

    @Override
    public boolean isPassParametersForExit() {
        return false;
    }

    public TransactionDefinition getDefinition() {
        return definition;
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionInterception that = (TransactionInterception)o;

        if (!definition.equals(that.definition)) {
            return false;
        }
        if (!handler.equals(that.handler)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = definition.hashCode();
        result = 31 * result + handler.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TransactionInterception{" +
            "definition=" + definition +
            ", noTransactionMethods=" + noTransactionMethods +
            ", handler=" + handler +
            '}';
    }
}
