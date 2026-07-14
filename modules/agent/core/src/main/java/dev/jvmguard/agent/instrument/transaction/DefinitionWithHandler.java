package dev.jvmguard.agent.instrument.transaction;

import dev.jvmguard.agent.callee.Handler;

public class DefinitionWithHandler {
    private final TransactionDefinition definition;
    private final Handler handler;

    public DefinitionWithHandler(TransactionDefinition definition, Handler handler) {
        this.definition = definition;
        this.handler = handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefinitionWithHandler that = (DefinitionWithHandler)o;

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
}
