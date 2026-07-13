package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CodecEntity;
import com.jvmguard.agent.config.base.AbstractEntity;

public abstract class NamingElement extends AbstractEntity implements AgentSerializable, CodecEntity {

    public abstract String getDisplayName();
    public abstract boolean canBeStatic();

    public boolean isIdentical(NamingElement namingElement) {
        return namingElement.getClass().equals(getClass());
    }
}
