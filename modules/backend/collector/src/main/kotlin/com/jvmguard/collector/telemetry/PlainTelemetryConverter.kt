package com.jvmguard.collector.telemetry

import com.jvmguard.agent.AgentConstants
import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.common.telemetry.AdditionalTelemetryManager
import com.jvmguard.data.vmdata.*
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier.Type

class PlainTelemetryConverter(
    private val telemetryManager: TelemetryManager,
    private val additionalTelemetryManager: AdditionalTelemetryManager,
) {

    fun createTelemetryData(mainId: String?, plainData: PlainTelemetryData, interval: TelemetryInterval, plainHeap: Boolean): TelemetryData {
        val ret = initTelemetryData(plainData)

        var rootNode: TelemetryNode? = null
        if (mainId != null) {
            rootNode = when (mainId) {
                Telemetry.HEAP.mainId -> getHeapData(plainData, plainHeap)
                Telemetry.TRANSACTIONS.mainId -> getTransactionData(plainData)
                Telemetry.CUSTOM.mainId -> getCustomData(plainData, null)
                else -> {
                    val data = plainData.subIdToData.values
                    if (data.size == 1) getSingleNode(mainId, data.iterator().next()) else null
                }
            }
        }
        if (rootNode == null) {
            if (Telemetry.CONNECTIONS.mainId == mainId && plainData.timeStamps != null) {
                rootNode = getEmptyConnectionsNode(plainData.timeStamps!!.size)
                ret.timestamps = plainData.timeStamps
            } else {
                rootNode = getSingleNode(mainId, LongArray(0))
                ret.timestamps = LongArray(0)
            }
        } else {
            ret.timestamps = plainData.timeStamps
        }

        ret.rootNode = rootNode
        ret.telemetryInterval = interval
        return ret
    }

    fun createCustomTelemetryData(nodeIdentifier: CustomTelemetryNodeIdentifier, plainData: PlainTelemetryData, interval: TelemetryInterval): TelemetryData {
        val ret = initTelemetryData(plainData)

        ret.timestamps = plainData.timeStamps
        val customData = getCustomData(plainData, nodeIdentifier)
        ret.rootNode = if (customData.children.isEmpty()) customData else customData.children.first()
        ret.telemetryInterval = interval
        return ret
    }

    private fun getTransactionData(plainData: PlainTelemetryData): TelemetryNode {
        val rootNode = TelemetryNode()

        val count = TelemetryNode("Transactions", true)
        count.setTelemetryUnit(TelemetryUnit.PER_SECOND, 4)

        count.addData("Error", TelemetryType.SUB_ID_ERROR, plainData.subIdToData[TelemetryType.SUB_ID_ERROR])
        count.addData("Very Slow", TelemetryType.SUB_ID_VERY_SLOW, plainData.subIdToData[TelemetryType.SUB_ID_VERY_SLOW])
        count.addData("Slow", TelemetryType.SUB_ID_SLOW, plainData.subIdToData[TelemetryType.SUB_ID_SLOW])
        count.addData("Normal", TelemetryType.SUB_ID_NORMAL, plainData.subIdToData[TelemetryType.SUB_ID_NORMAL])

        val average = TelemetryNode("Average Transaction Duration", false)
        average.setTelemetryUnit(TelemetryUnit.NANOSECONDS, 0)

        average.addData("Average Duration", TelemetryType.SUB_ID_AVERAGE, plainData.subIdToData[TelemetryType.SUB_ID_AVERAGE])

        rootNode.children.add(count)
        rootNode.children.add(average)
        return rootNode
    }

    private fun getHeapData(plainData: PlainTelemetryData, plain: Boolean): TelemetryNode {
        val rootNode = TelemetryNode()

        val heapNode = TelemetryNode("Heap", true)
        heapNode.setTelemetryUnit(TelemetryUnit.BYTES, 0)
        heapNode.addData("Used", TelemetryType.SUB_ID_USED_HEAP, plainData.subIdToData[TelemetryType.SUB_ID_USED_HEAP])
        heapNode.addData("Free", TelemetryType.SUB_ID_FREE_HEAP, plainData.subIdToData[TelemetryType.SUB_ID_FREE_HEAP])

        val nonHeapNode = TelemetryNode("Non-Heap", true)
        nonHeapNode.setTelemetryUnit(TelemetryUnit.BYTES, 0)

        val subIdToNode = HashMap<String, TelemetryNode>()
        for ((key, value) in plainData.subIdToData) {
            if (key != TelemetryType.SUB_ID_USED_HEAP && key != TelemetryType.SUB_ID_FREE_HEAP) {
                val additionalTelemetry = additionalTelemetryManager.getVisibleTelemetry(key)
                if (additionalTelemetry != null && (additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_USED || additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MEMORY_NON_HEAP_USED)) {
                    val node = TelemetryNode(additionalTelemetry.nodeName, true)
                    node.setTelemetryUnit(TelemetryUnit.BYTES, 0)
                    node.addData("Used", key, value)
                    val telemetryName = additionalTelemetry.name ?: ""
                    val committedTelemetry = if (additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_USED) {
                        heapNode.children.add(node)
                        additionalTelemetryManager.getOrCreateAdditionalTelemetry(AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_COMMITTED, telemetryName)
                    } else {
                        nonHeapNode.children.add(node)
                        additionalTelemetryManager.getOrCreateAdditionalTelemetry(AgentConstants.TELEMETRY_TYPE_MEMORY_NON_HEAP_COMMITTED, telemetryName)
                    }
                    if (committedTelemetry != null) {
                        subIdToNode[committedTelemetry.assignedStringId] = node
                    }
                }
            }
        }
        for ((key, value) in plainData.subIdToData) {
            if (key != TelemetryType.SUB_ID_USED_HEAP && key != TelemetryType.SUB_ID_FREE_HEAP) {
                val additionalTelemetry = additionalTelemetryManager.getVisibleTelemetry(key)
                if (additionalTelemetry != null && (additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_COMMITTED || additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MEMORY_NON_HEAP_COMMITTED)) {
                    subIdToNode[key]?.addData("Free", key, value)
                }
            }
        }
        calcFree(heapNode)
        calcFree(nonHeapNode)

        val nonHeapUsedData: LongArray?
        val nonHeapFreeData: LongArray?
        if (nonHeapNode.children.isNotEmpty()) {
            nonHeapUsedData = LongArray(plainData.timeStamps!!.size) { Long.MIN_VALUE }
            nonHeapFreeData = LongArray(plainData.timeStamps!!.size) { Long.MIN_VALUE }
            for (child in nonHeapNode.children) {
                val usedData = child.data[0].data ?: continue
                val freeData = child.data[1].data ?: continue
                for (i in nonHeapUsedData.indices) {
                    val used = usedData[i]
                    if (used > Long.MIN_VALUE) {
                        if (nonHeapUsedData[i] == Long.MIN_VALUE) {
                            nonHeapUsedData[i] = 0
                        }
                        nonHeapUsedData[i] += used
                    }
                    val free = freeData[i]
                    if (free > Long.MIN_VALUE) {
                        if (nonHeapFreeData[i] == Long.MIN_VALUE) {
                            nonHeapFreeData[i] = 0
                        }
                        nonHeapFreeData[i] += free
                    }
                }
            }
        } else {
            nonHeapUsedData = null
            nonHeapFreeData = null
        }
        nonHeapNode.addData("Used", "", nonHeapUsedData)
        nonHeapNode.addData("Free", "", nonHeapFreeData)

        if (plain) {
            rootNode.setTelemetryUnit(TelemetryUnit.BYTES, 0)
            rootNode.description = Telemetry.HEAP.toString()
            addPlainHeapNode(rootNode, heapNode)
            addPlainHeapNode(rootNode, nonHeapNode)
        } else {
            rootNode.children.add(heapNode)
            rootNode.children.add(nonHeapNode)
        }
        return rootNode
    }

    private fun addPlainHeapNode(rootNode: TelemetryNode, heapNode: TelemetryNode) {
        for (data in heapNode.data) {
            rootNode.addData(data.description + " " + heapNode.description, data.subId, data.data)
        }
        for (childNode in heapNode.children) {
            for (data in childNode.data) {
                rootNode.addData(data.description + " " + childNode.description, data.subId, data.data)
            }
        }
    }

    private fun getCustomData(plainData: PlainTelemetryData, nodeIdentifier: CustomTelemetryNodeIdentifier?): TelemetryNode {
        val rootNode = TelemetryNode(TelemetryNode.CUSTOM_TELEMETRIES_TEXT, true)

        for ((key, value) in plainData.subIdToData) {
            val additionalTelemetry = additionalTelemetryManager.getVisibleTelemetry(key)
            if (additionalTelemetry != null && (nodeIdentifier == null || nodeIdentifier.name == additionalTelemetry.nodeName)) {
                val devops = additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_DEVOPS && (nodeIdentifier == null || nodeIdentifier.type == Type.DEVOPS)
                val mbean = additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_MBEAN && (nodeIdentifier == null || nodeIdentifier.type == Type.MBEAN)
                if (devops || mbean) {
                    var node = rootNode.children.firstOrNull { it.description == additionalTelemetry.nodeName }
                    if (node == null) {
                        if (mbean) {
                            val format = telemetryManager.getMbeanTelemetryFormat(additionalTelemetry.nodeName)
                            if (format != null) {
                                node = TelemetryNode(additionalTelemetry.nodeName, format.stacked)
                                node.setTelemetryUnit(
                                    TelemetryUnit.fromAnnotationUnit(format.value),
                                    TelemetryManager.CUSTOM_TELEMETRY_BASE_SCALE + format.scale
                                )
                            }
                        } else {
                            val format = additionalTelemetryManager.getFormat(additionalTelemetry)
                            node = TelemetryNode(additionalTelemetry.nodeName, format.stacked)
                            node.setTelemetryUnit(TelemetryUnit.fromAnnotationUnit(format.value), TelemetryManager.CUSTOM_TELEMETRY_BASE_SCALE + format.scale)
                        }

                        if (node != null) {
                            rootNode.children.add(node)
                        }
                    }
                    node?.addData(additionalTelemetry.lineName, key, value)
                }
            }
        }

        return rootNode
    }

    private fun calcFree(node: TelemetryNode) {
        val iterator = node.children.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            if (child.data.size != 2) {
                iterator.remove()
            } else {
                val free = child.data[1].data
                val used = child.data.first().data
                if (free != null && used != null) {
                    for (i in free.indices) {
                        if (free[i] != Long.MIN_VALUE) {
                            free[i] -= used[i]
                        }
                    }
                }
            }
        }
    }

    private fun getEmptyConnectionsNode(count: Int): TelemetryNode {
        val node = TelemetryNode()
        node.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
        node.description = CONNECTED_VMS_DESCRIPTION
        node.addData(CONNECTED_VMS_DESCRIPTION, "", LongArray(count))
        return node
    }

    private fun getSingleNode(mainId: String?, data: LongArray?): TelemetryNode {
        val node = TelemetryNode()
        when (mainId) {
            Telemetry.CPU.mainId -> {
                node.setTelemetryUnit(TelemetryUnit.PERCENT, 2)
                node.description = "CPU"
                node.addData("CPU load", "", data)
            }

            Telemetry.GC.mainId -> {
                node.setTelemetryUnit(TelemetryUnit.PERCENT, 2)
                node.description = "GC Activity"
                node.addData("GC Activity", "", data)
            }

            Telemetry.THREADS.mainId -> {
                node.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
                node.description = "Threads"
                node.addData("Thread Count", "", data)
            }

            Telemetry.CONNECTIONS.mainId -> {
                node.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
                node.description = CONNECTED_VMS_DESCRIPTION
                if (data != null) {
                    for (i in data.indices) {
                        if (data[i] == Long.MIN_VALUE) {
                            data[i] = 0
                        }
                    }
                }
                node.addData(CONNECTED_VMS_DESCRIPTION, "", data)
            }

            else -> {
                node.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
                node.description = "Unknown"
                node.addData("Unknown", "", data)
            }
        }
        return node
    }

    private fun TelemetryNode.addData(description: String, subId: String, values: LongArray?): TelemetryNode.Data =
        if (values != null) addData(description, subId, values) else addData(description, subId)

    companion object {
        private const val CONNECTED_VMS_DESCRIPTION = "Connected VMs"

        fun initTelemetryData(plainData: PlainTelemetryData): TelemetryData {
            val ret = TelemetryData()
            ret.isNoPreviousData = plainData.isNoPreviousData
            ret.dataInterval = plainData.dataInterval
            return ret
        }
    }
}
