package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.policy.PolicyHandler;

public interface PolicyDef {
    boolean isDiscard();
    void setDiscard(boolean discard);
    Policy getPolicy();
    void setPolicy(Policy policy);
    PolicyHandler initPolicyHandler();
    PolicyHandler getPolicyHandler();
}
