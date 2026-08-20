package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Key

class AttributeEditDialog(
    spec: ValueEditSpec,
    private val onCommit: (Any?) -> Unit,
) : JvmGuardDialog() {

    private val form = MBeanValuesForm(listOf(spec))

    init {
        headerTitle = t("mbeans.attribute.edit.title")
        width = "32rem"

        add(form)
        val save = confirmFooter(t("common.save"), ID_SAVE) { save() }
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
