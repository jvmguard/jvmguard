package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.comm.AgentSerializable;
import dev.jvmguard.agent.comm.CodecEntity;
import dev.jvmguard.agent.config.base.AbstractEntity;

public abstract class NamingElement extends AbstractEntity implements AgentSerializable, CodecEntity {

    public abstract String getDisplayName();
    public abstract boolean canBeStatic();

    public boolean isIdentical(NamingElement namingElement) {
        return namingElement.getClass().equals(getClass());
    }
}
