package dev.jvmguard.common.export

import dev.jvmguard.common.export.base.AbstractExport
import dev.jvmguard.data.vmdata.TelemetryNode
import dev.jvmguard.data.vmdata.TelemetryNode.Data
import java.math.BigDecimal
import java.util.*

class TelemetryExport(private val timeStamps: LongArray?, private val telemetryNode: TelemetryNode) :
    AbstractExport<TelemetryExport>("data") {

    init {
        addProperty("description", telemetryNode.description)
        addProperty("unit", telemetryNode.telemetryUnit.getLabel(0))
    }

    override fun doExport() {
        val generator = gen!!
        if (timeStamps != null) {
            val dataToScaledValues = IdentityHashMap<Data, List<BigDecimal?>>()
            for (dataIndex in telemetryNode.data.indices) {
                val data = telemetryNode.data[dataIndex]
                dataToScaledValues[data] = data.plainScaledData
            }
            for (i in timeStamps.indices) {
                val timeStamp = timeStamps[i]
                generator.writeStartObject()
                    .write("time", timeStamp)

                for (dataIndex in telemetryNode.data.indices) {
                    val data = telemetryNode.data[dataIndex]
                    val scaledValues = dataToScaledValues[data]
                    try {
                        if (scaledValues != null) {
                            if (scaledValues[i] == null) {
                                generator.writeNull(data.description)
                            } else {
                                generator.write(data.description, scaledValues[i])
                            }
                        }
                    } catch (_: ArrayIndexOutOfBoundsException) {
                        generator.writeNull(data.description)
                    }
                }
                generator.writeEndObject()
            }
        }
    }

    override fun isArray(): Boolean = true
}
