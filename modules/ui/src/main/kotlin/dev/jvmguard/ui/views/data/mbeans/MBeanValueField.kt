package dev.jvmguard.ui.views.data.mbeans

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.textfield.TextField
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import javax.management.openmbean.ArrayType
import javax.management.openmbean.SimpleType

class MBeanValueField(private val spec: ValueEditSpec) {

    val component: Component

    private val checkbox: Checkbox?
    private val dateField: DateTimePicker?
    private val textField: TextField?

    init {
        when {
            spec.isSimpleType(SimpleType.BOOLEAN) -> {
                checkbox = Checkbox(spec.caption).apply { value = spec.initialValue as? Boolean ?: false }
                dateField = null
                textField = null
                component = checkbox
            }

            spec.isSimpleType(SimpleType.DATE) -> {
                dateField = DateTimePicker(spec.caption).apply {
                    step = Duration.ofSeconds(1)
                    setWidthFull()
                    value = (spec.initialValue as? Date)?.toLocalDateTime()
                }
                checkbox = null
                textField = null
                component = dateField
            }

            else -> {
                textField = TextField(spec.caption).apply {
                    isClearButtonVisible = true
                    setWidthFull()
                    value = initialText()
                    arrayHelperText()?.let { helperText = it }
                }
                checkbox = null
                dateField = null
                component = textField
            }
        }
    }

    fun readValue(): Any? = when {
        checkbox != null -> checkbox.value
        dateField != null -> dateField.value?.toDate()
        else -> parseText(textField!!.value)
    }

    fun clearInvalid() {
        textField?.isInvalid = false
        dateField?.isInvalid = false
    }

    fun markInvalid(message: String) {
        textField?.apply { isInvalid = true; errorMessage = message }
        dateField?.apply { isInvalid = true; errorMessage = message }
    }

    private fun parseText(raw: String): Any? {
        val openType = spec.openType
        if (openType is ArrayType<*> && OpenTypeHelper.isEditable(openType)) {
            val componentClass = Class.forName(openType.elementOpenType.typeName)
            return ObjectArrayHelper.convertToArray(raw, componentClass, !openType.isPrimitiveArray)
        }
        val text = raw.trim()
        if (text.isEmpty()) {
            return when {
                spec.isSimpleType(SimpleType.STRING) -> ""
                spec.isNullable -> null
                else -> throw MBeanConversionException("A value is required")
            }
        }
        return when {
            spec.isSimpleType(SimpleType.INTEGER) -> convert(text) { it.toInt() }
            spec.isSimpleType(SimpleType.LONG) -> convert(text) { it.toLong() }
            spec.isSimpleType(SimpleType.SHORT) -> convert(text) { it.toShort() }
            spec.isSimpleType(SimpleType.BYTE) -> convert(text) { it.toByte() }
            spec.isSimpleType(SimpleType.FLOAT) -> convert(text) { it.toFloat() }
            spec.isSimpleType(SimpleType.DOUBLE) -> convert(text) { it.toDouble() }
            spec.isSimpleType(SimpleType.BIGINTEGER) -> convert(text) { BigInteger(it) }
            spec.isSimpleType(SimpleType.BIGDECIMAL) -> convert(text) { BigDecimal(it) }
            spec.isSimpleType(SimpleType.CHARACTER) -> raw.first()
            spec.isSimpleType(SimpleType.OBJECTNAME) -> parseObjectName(text)
            else -> raw
        }
    }

    private inline fun <T> convert(text: String, parse: (String) -> T): T =
        try {
            parse(text)
        } catch (_: NumberFormatException) {
            throw MBeanConversionException("\"$text\" cannot be converted to ${spec.type}")
        }

    private fun parseObjectName(text: String): ObjectName =
        try {
            ObjectName(text)
        } catch (e: MalformedObjectNameException) {
            throw MBeanConversionException("Not a valid ObjectName: ${e.message}")
        }

    private fun initialText(): String {
        val initial = spec.initialValue ?: return ""
        if (spec.openType is ArrayType<*> && initial is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            return runCatching { ObjectArrayHelper.convertToString(initial as Array<Any?>) }.getOrNull() ?: ""
        }
        return initial.toString()
    }

    private fun arrayHelperText(): String? {
        val openType = spec.openType
        if (openType !is ArrayType<*> || !OpenTypeHelper.isEditable(openType)) {
            return null
        }
        val elementType = openType.elementOpenType.typeName.substringAfterLast('.')
        val quoting = if (openType.elementOpenType == SimpleType.STRING) {
            " Quote empty strings or strings containing semicolons."
        } else {
            ""
        }
        return "Array of $elementType. Separate elements with semicolons, e.g. \"A;B;C\".$quoting"
    }

    companion object {
        private fun Date.toLocalDateTime(): LocalDateTime =
            LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault())

        private fun LocalDateTime.toDate(): Date =
            Date.from(atZone(ZoneId.systemDefault()).toInstant())
    }
}
