package com.jvmguard.ui.server

import com.jvmguard.data.vmdata.VmIdentifier
import com.vaadin.flow.shared.Registration

class VmSelectionModel(initial: VmIdentifier = VmIdentifier.ROOT_GROUP_IDENTIFIER) {

    var selection: VmIdentifier = initial
        private set

    private val listeners = mutableListOf<(VmIdentifier) -> Unit>()

    fun set(newSelection: VmIdentifier) {
        if (newSelection == selection) {
            return
        }
        selection = newSelection
        listeners.toList().forEach { it(newSelection) }
    }

    fun addListener(listener: (VmIdentifier) -> Unit): Registration {
        listeners.add(listener)
        return Registration { listeners.remove(listener) }
    }
}
