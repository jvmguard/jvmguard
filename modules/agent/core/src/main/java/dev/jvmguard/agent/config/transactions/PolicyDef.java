package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.policy.PolicyHandler;

public interface PolicyDef {
    boolean isDiscard();
    void setDiscard(boolean discard);
    Policy getPolicy();
    void setPolicy(Policy policy);
    PolicyHandler initPolicyHandler();
    PolicyHandler getPolicyHandler();
}
