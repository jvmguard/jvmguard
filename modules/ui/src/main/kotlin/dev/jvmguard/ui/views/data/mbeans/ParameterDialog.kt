package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import javax.management.MBeanOperationInfo

class ParameterDialog(
    operationInfo: MBeanOperationInfo,
    specs: List<ValueEditSpec>,
    private val onInvoke: (List<Any?>) -> Unit,
) : JvmGuardDialog() {

    private val form = MBeanValuesForm(specs)

    init {
        headerTitle = "Invoke operation"
        width = "36rem"

        val signature = Span(operationInfo.name + MBeanOperations.signatureSuffix(operationInfo)).apply {
            addClassName("jvmguard-mbean-operation-signature")
        }
        val invoke = Button("Invoke") { invoke() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_INVOKE
        }
        invoke.addClickShortcut(Key.ENTER).listenOn(this)

        add(VerticalLayout(signature, form).apply {
            isPadding = false
            isSpacing = false
            style.set("gap", "0.75rem")
        })
        footer.add(Button("Cancel") { close() }, invoke)
    }

    private fun invoke() {
        val values = form.readValues() ?: return
        onInvoke(values)
        close()
    }

    companion object {
        const val ID_INVOKE = "mbean-operation-invoke"
    }
}
