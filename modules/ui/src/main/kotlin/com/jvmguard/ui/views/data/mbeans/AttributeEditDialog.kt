package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.Key

class AttributeEditDialog(
    spec: ValueEditSpec,
    private val onCommit: (Any?) -> Unit,
) : JvmGuardDialog() {

    private val form = MBeanValuesForm(listOf(spec))

    init {
        headerTitle = "Edit attribute"
        width = "32rem"

        add(form)
        val save = confirmFooter("Save", ID_SAVE) { save() }
        save.addClickShortcut(Key.ENTER).listenOn(this)
    }

    private fun save() {
        val values = form.readValues() ?: return
        onCommit(values.first())
        close()
    }

    companion object {
        const val ID_SAVE = "mbean-attribute-save"
    }
}
