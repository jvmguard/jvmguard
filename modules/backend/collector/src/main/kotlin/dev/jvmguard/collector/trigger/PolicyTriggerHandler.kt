package dev.jvmguard.collector.trigger

import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.vmdata.VM

class PolicyTriggerHandler(trigger: PolicyTrigger, groupVm: VM) : TriggerHandler(trigger, groupVm) {

    override fun getTrigger(): PolicyTrigger {
        return super.getTrigger() as PolicyTrigger
    }

    override fun toString(): String {
        return "handler for ${getTrigger()}"
    }
}
