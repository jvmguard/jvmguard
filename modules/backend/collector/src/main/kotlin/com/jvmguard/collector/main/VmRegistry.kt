package com.jvmguard.collector.main

import com.jvmguard.collector.vmdata.VmGroupData
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import org.springframework.stereotype.Component

@Component
class VmRegistry(
    private val vmStorage: VmStorage,
    private val configManager: ConfigManager,
) {
    private val groupVms = HashMap<VmIdentifier, VM>()

    @Volatile
    private var rootVmGroupDataField: VmGroupData? = null

    var rootVmGroupData: VmGroupData
        get() = rootVmGroupDataField!!
        internal set(value) {
            rootVmGroupDataField = value
        }

    fun getRootGroupVM(): VM {
        return getGroupVM(VmIdentifier.ROOT_GROUP_IDENTIFIER)
    }

    fun getGroupVM(groupIdentifier: VmIdentifier): VM {
        synchronized(groupVms) {
            var vm = groupVms[groupIdentifier]
            if (vm == null) {
                val parentIdentifier = groupIdentifier.parent
                vm = vmStorage.getVm(groupIdentifier.toUnqualified().name, parentIdentifier?.name ?: "", 0, groupIdentifier.type, "", 0)!!
                configManager.groupConnected(vm.parentIdentifier)
                groupVms[groupIdentifier] = vm
            }
            return vm
        }
    }
}
