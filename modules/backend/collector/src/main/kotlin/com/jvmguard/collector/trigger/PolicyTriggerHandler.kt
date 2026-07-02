package com.jvmguard.collector.trigger

import com.jvmguard.data.config.triggers.PolicyTrigger
import com.jvmguard.data.vmdata.VM

class PolicyTriggerHandler(trigger: PolicyTrigger, groupVm: VM) : TriggerHandler(trigger, groupVm) {

    override fun getTrigger(): PolicyTrigger {
        return super.getTrigger() as PolicyTrigger
    }

    override fun toString(): String {
        return "handler for ${getTrigger()}"
    }
}
