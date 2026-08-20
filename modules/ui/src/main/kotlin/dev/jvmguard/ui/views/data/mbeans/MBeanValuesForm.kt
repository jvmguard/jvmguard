package dev.jvmguard.ui.views.data.mbeans

import com.vaadin.flow.component.formlayout.FormLayout
import dev.jvmguard.ui.server.t

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
                field.markInvalid(e.message ?: t("mbeans.value.invalid"))
                valid = false
            }
        }
        return if (valid) values else null
    }
}
