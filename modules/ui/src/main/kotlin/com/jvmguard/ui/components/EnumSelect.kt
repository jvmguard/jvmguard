package com.jvmguard.ui.components

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.ItemLabelGenerator
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.select.Select

@JsModule("./enum-select.ts")
class EnumSelect<E : Enum<E>>(
    label: String, enumType: Class<E>, labelGenerator: ItemLabelGenerator<E>
) : Select<E>() {

    private val itemLabels: List<String> = enumType.enumConstants.map { labelGenerator.apply(it) }

    init {
        setLabel(label)
        setItems(*enumType.enumConstants)
        setItemLabelGenerator(labelGenerator)
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        // Auto-size to the widest option unless an explicit width is set
        if (width == null) {
            measureWidth()
        }
    }

    private fun measureWidth() {
        element.executeJs($$"window.JvmGuard.autoWidthSelect(this, $0, $1)", itemLabels.joinToString(SEPARATOR), SEPARATOR)
    }

    fun addUserValueChangeListener(onChange: () -> Unit) {
        addValueChangeListener { event -> if (event.isFromClient) onChange() }
    }

    companion object {
        private const val SEPARATOR = "\t"
    }
}
