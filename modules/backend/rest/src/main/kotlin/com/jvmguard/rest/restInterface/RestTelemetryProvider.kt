package com.jvmguard.rest.restInterface

import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.collector.api.TelemetryProvider
import com.jvmguard.common.export.TelemetryExport
import com.jvmguard.common.export.base.AbstractExport
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier.Type
import com.jvmguard.data.vmdata.Telemetry
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.rest.RestHelper
import com.jvmguard.rest.entity.TelemetryDescriptor
import com.jvmguard.rest.entity.TelemetryDescriptor.TelemetryType
import com.jvmguard.rest.provider.RestException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RestTelemetryProvider(private val telemetryProvider: TelemetryProvider) {

    fun getTelemetryDescriptors(): List<TelemetryDescriptor> {
        val ret = ArrayList<TelemetryDescriptor>()

        for (telemetry in Telemetry.entries) {
            when {
                telemetry == Telemetry.TRANSACTIONS -> {
                    ret.add(TelemetryDescriptor("${telemetry.exportDescriptor}/$FREQUENCY", TelemetryType.BASE, "$telemetry (Frequency)"))
                    ret.add(TelemetryDescriptor("${telemetry.exportDescriptor}/$AVERAGE", TelemetryType.BASE, "$telemetry (Average Duration)"))
                }

                telemetry != Telemetry.CUSTOM -> {
                    ret.add(TelemetryDescriptor(telemetry.exportDescriptor, TelemetryType.BASE, telemetry.toString()))
                }
            }
        }

        val customTelemetries = ArrayList<TelemetryDescriptor>()
        for (nodeIdentifier in telemetryProvider.customTelemetryInfo.customTelemetryNodeIdentifiers) {
            val type = if (nodeIdentifier.type == Type.DECLARED) TelemetryType.DECLARED else TelemetryType.MBEAN
            customTelemetries.add(TelemetryDescriptor(nodeIdentifier.name, type, nodeIdentifier.name))
        }

        customTelemetries.sortBy { it.name }
        ret.addAll(customTelemetries)

        return ret
    }

    fun getTelemetryExport(vm: VM, telemetryName: String, telemetryInterval: TelemetryInterval, endTime: Long): TelemetryExport {
        val currentTime = System.currentTimeMillis()
        val effectiveEndTime = if (endTime > currentTime) currentTime else endTime

        val telemetryExport = when {
            telemetryName.startsWith(TelemetryType.DECLARED.namePrefix) -> getCustomTelemetry(
                CustomTelemetryNodeIdentifier(Type.DECLARED, telemetryName.substring(TelemetryType.DECLARED.namePrefix.length)),
                telemetryInterval, vm, effectiveEndTime
            )

            telemetryName.startsWith(TelemetryType.MBEAN.namePrefix) -> getCustomTelemetry(
                CustomTelemetryNodeIdentifier(Type.MBEAN, telemetryName.substring(TelemetryType.MBEAN.namePrefix.length)),
                telemetryInterval, vm, effectiveEndTime
            )

            else -> getBaseTelemetry(telemetryName, telemetryInterval, vm, effectiveEndTime)
        } ?: throw RestException("telemetry data $telemetryName not found", HttpStatus.NOT_FOUND)

        telemetryExport.addProperty(AbstractExport.PROPNAME_END_TIME, Instant.ofEpochMilli(effectiveEndTime))
        telemetryExport.addProperty(AbstractExport.PROPNAME_INTERVAL, telemetryInterval.timeExtent)
        return RestHelper.addVmProperties(telemetryExport, vm)
    }

    private fun getBaseTelemetry(telemetryName: String, telemetryInterval: TelemetryInterval, vm: VM, endTime: Long): TelemetryExport? {
        var searchedTelemetry: Telemetry? = null
        var subName: String? = null
        for (telemetry in Telemetry.entries) {
            if (telemetry.exportDescriptor == telemetryName) {
                searchedTelemetry = telemetry
                break
            } else if (telemetryName.startsWith(telemetry.exportDescriptor + "/")) {
                searchedTelemetry = telemetry
                subName = telemetryName.substring(telemetry.exportDescriptor.length + 1)
                break
            }
        }
        if (searchedTelemetry != null) {
            val telemetryData = telemetryProvider.getTelemetryData(vm, searchedTelemetry.mainId, telemetryInterval, endTime, true)
            val rootNode = telemetryData.rootNode ?: return null
            if (searchedTelemetry == Telemetry.TRANSACTIONS) {
                for (childNode in rootNode.children) {
                    if ((FREQUENCY == subName && childNode.telemetryUnit == TelemetryUnit.PER_SECOND) ||
                        (AVERAGE == subName && childNode.telemetryUnit == TelemetryUnit.NANOSECONDS)
                    ) {
                        return TelemetryExport(telemetryData.timestamps, childNode)
                    }
                }
            } else if (subName == null) {
                return TelemetryExport(telemetryData.timestamps, rootNode)
            }
        }
        return null
    }

    private fun getCustomTelemetry(
        nodeIdentifier: CustomTelemetryNodeIdentifier,
        telemetryInterval: TelemetryInterval,
        vm: VM,
        endTime: Long
    ): TelemetryExport? {
        if (telemetryProvider.customTelemetryInfo.customTelemetryNodeIdentifiers.contains(nodeIdentifier)) {
            val telemetryData = telemetryProvider.getCustomTelemetryData(vm, nodeIdentifier, telemetryInterval, endTime)
            val rootNode = telemetryData.rootNode ?: return null
            return TelemetryExport(telemetryData.timestamps, rootNode)
        }
        return null
    }

    companion object {
        private const val FREQUENCY = "frequency"
        private const val AVERAGE = "average"
    }
}
