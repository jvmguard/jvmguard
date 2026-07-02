package com.jvmguard.ui.views.data.mbeans

import com.vaadin.flow.component.formlayout.FormLayout

class MBeanValuesForm(specs: List<ValueEditSpec>) : FormLayout() {

    private val fields = specs.map { MBeanValueField(it) }

    init {
        responsiveSteps = listOf(ResponsiveStep("0", 1))
        fields.forEach { add(it.component) }
    }

    fun readValues(): List<Any?>? {
        var valid = true
        val values = ArrayList<Any?>(fields.size)
        for (field in fields) {
            field.clearInvalid()
            try {
                values.add(field.readValue())
            } catch (e: MBeanConversionException) {
                field.markInvalid(e.message ?: "Invalid value")
                valid = false
            }
        }
        return if (valid) values else null
    }
}
