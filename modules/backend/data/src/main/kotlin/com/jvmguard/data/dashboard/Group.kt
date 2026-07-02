package com.jvmguard.data.dashboard

import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier

class Group<T> @JvmOverloads constructor(
    @field:Transient private val dataClass: Class<*>? = null,
    val parent: Group<T>? = null,
) {
    val groupChildren: MutableMap<VmIdentifier, Group<T>> = HashMap()
    val vmDataMap: MutableMap<VM, T> = HashMap()

    var data: T? = newData()

    fun getOrCreateGroupChild(vmIdentifier: VmIdentifier): Group<T> =
        groupChildren.computeIfAbsent(vmIdentifier) { _ -> Group(dataClass, this) }

    fun setVmData(vm: VM, vmData: T) {
        vmDataMap[vm] = vmData
    }

    fun removeGroupChild(vmIdentifier: VmIdentifier) {
        groupChildren.remove(vmIdentifier)
    }

    val isEmpty: Boolean
        get() = vmDataMap.isEmpty() && groupChildren.isEmpty()

    @Suppress("UNCHECKED_CAST")
    private fun newData(): T? {
        if (dataClass == null) {
            return null
        }
        return try {
            dataClass.getDeclaredConstructor().newInstance() as T
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    override fun toString(): String =
        "Group{" +
                "groupChildren=" + groupChildren +
                ", vmDataMap=" + vmDataMap +
                ", data=" + data +
                '}'
}
