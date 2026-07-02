package com.jvmguard.integration.util

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryNode
import org.jdom2.Element
import java.math.BigDecimal

val TelemetryData.nonNullRootNode: TelemetryNode get() = requireNotNull(rootNode)
val TelemetryData.nonNullTimestamps: LongArray get() = requireNotNull(timestamps)

val TelemetryNode.Data.nonNullUnitScaledData: List<BigDecimal?> get() = requireNotNull(unitScaledData)
val TelemetryNode.Data.nonNullPlainScaledData: List<BigDecimal?> get() = requireNotNull(plainScaledData)

fun <T> JsonObject.nonNullArray(fieldName: String): JsonArray<T> = requireNotNull(array(fieldName))
fun JsonObject.nonNullLong(fieldName: String): Long = requireNotNull(long(fieldName))
fun JsonObject.nonNullString(fieldName: String): String = requireNotNull(string(fieldName))
fun JsonObject.nonNullBoolean(fieldName: String): Boolean = requireNotNull(boolean(fieldName))
fun JsonObject.nonNullObj(fieldName: String): JsonObject = requireNotNull(obj(fieldName))

fun Element.nonNullAttr(name: String): String = requireNotNull(attr(name))
