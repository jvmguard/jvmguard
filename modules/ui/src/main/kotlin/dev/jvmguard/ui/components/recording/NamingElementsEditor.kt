package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.NamingElement
import dev.jvmguard.agent.config.transactions.naming.*
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField

class NamingElementsEditor : VerticalLayout() {

    private var elements: MutableList<NamingElement> = mutableListOf()

    private val grid = Grid(NamingElement::class.java, false).apply {
        testId = ID_GRID
        addColumn { displayNameOf(it) }.setHeader(t("recording.naming.element")).setFlexGrow(1)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::remove)
        enableRowReorder(items = { elements }, onReordered = ::refresh)
        setSizeFull()
        minHeight = "8rem"
    }

    init {
        isPadding = false
        isSpacing = true
        setSizeFull()
        val hint = Span(t("recording.naming.hint"))
        val addButton = menuButton(VaadinIcon.PLUS, t("recording.naming.add"), ID_ADD) {
            ELEMENT_TYPES.forEach { type -> addItem(t(type.labelKey)) { addElement(type) } }
        }
        val header = HorizontalLayout(hint, addButton).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            setFlexGrow(1.0, hint)
        }
        add(header, grid)
        setFlexGrow(1.0, grid)
    }

    fun setElements(elements: MutableList<NamingElement>) {
        this.elements = elements
        refresh()
    }

    private fun addElement(type: ElementType) {
        val element = type.create()
        elements.add(element)
        refresh()
        if (type.hasDialog) {
            edit(element)
        }
    }

    private fun edit(element: NamingElement) {
        if (!hasDialog(element)) {
            return
        }
        NamingElementDialog(element) { refresh() }.open()
    }

    private fun rowActions(element: NamingElement): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("recording.actions"), "$ID_ROW_MENU-${elements.indexOf(element)}") {
            if (hasDialog(element)) {
                addItem(t("common.edit")) { edit(element) }
            }
            addItem(t("common.remove")) { remove(element) }
        }

    private fun remove(element: NamingElement) {
        elements.remove(element)
        refresh()
    }

    private fun refresh() {
        grid.setItems(elements)
    }

    private fun displayNameOf(element: NamingElement): String = when (element) {
        is InstanceClassNameElement -> t("recording.naming.row.instanceClassName." + element.packageMode.name.lowercase())
        is ClassNameElement -> t("recording.naming.row.className." + element.packageMode.name.lowercase())
        is MethodParameterElement -> withGetterChain(element, "recording.naming.row.methodParameter", element.parameterIndex)
        is InstanceElement -> withGetterChain(element, "recording.naming.row.instance")
        is MethodNameElement -> t("recording.naming.type.methodName")
        is TextElement -> t("recording.naming.row.text", element.text)
        else -> element.displayName
    }

    private fun withGetterChain(element: AbstractGetterElement, key: String, vararg params: Any): String {
        val chain = element.getterChain.usedValue
        return if (chain.isEmpty()) t(key, *params) else t("$key.getterChain", *params, chain)
    }

    private enum class ElementType(val labelKey: String, val create: () -> NamingElement, val hasDialog: Boolean) {
        CLASS_NAME("recording.naming.type.className", { ClassNameElement() }, true),
        INSTANCE_CLASS_NAME("recording.naming.type.instanceClassName", { InstanceClassNameElement() }, true),
        INSTANCE("recording.naming.type.instance", { InstanceElement() }, true),
        METHOD_PARAMETER("recording.naming.type.methodParameter", { MethodParameterElement() }, true),
        METHOD_NAME("recording.naming.type.methodName", { MethodNameElement() }, false),
        TEXT("recording.naming.type.text", { TextElement() }, true),
    }

    companion object {
        const val ID_GRID = "naming-elements-grid"
        const val ID_ADD = "naming-elements-add"
        const val ID_ROW_MENU = "naming-element-row-menu"

        private val ELEMENT_TYPES = ElementType.entries

        private fun hasDialog(element: NamingElement): Boolean = element !is MethodNameElement
    }
}

private class NamingElementDialog(
    private val element: NamingElement,
    private val onSave: () -> Unit,
) : JvmGuardDialog() {

    init {
        headerTitle = t("recording.naming.element")
        width = "34rem"

        val body = VerticalLayout().apply { isPadding = false; isSpacing = true }
        val apply: () -> Boolean = buildFields(body)
        add(body)

        val cancel = Button(t("common.cancel")) { close() }
        val save = Button(t("common.save")) {
            if (apply()) {
                onSave()
                close()
            }
        }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = "naming-element-save"
        }
        footer.add(cancel, save)
    }

    private fun buildFields(body: VerticalLayout): () -> Boolean {
        when (val e = element) {
            is TextElement -> {
                val text = TextField(t("recording.naming.text")).apply { setWidthFull(); value = e.text }
                body.add(text)
                return { e.text = text.value; true }
            }

            is InstanceClassNameElement -> return packageModeField(body, e)
            is ClassNameElement -> return packageModeField(body, e)
            is InstanceElement -> {
                val getter = getterChainField(e.getterChain.usedValue)
                body.add(getter)
                return { applyGetterChain(e.getterChain, getter); true }
            }

            is MethodParameterElement -> {
                val index = IntegerField(t("recording.naming.parameterIndex")).apply { width = "12rem"; value = e.parameterIndex }
                val getter = getterChainField(e.getterChain.usedValue)
                body.add(index, getter)
                return { e.parameterIndex = index.value ?: 0; applyGetterChain(e.getterChain, getter); true }
            }

            else -> return { true }
        }
    }

    private fun packageModeField(body: VerticalLayout, element: ClassNameElement): () -> Boolean {
        val mode = EnumSelect(t("recording.naming.packageName"), ClassNameElement.PackageMode::class.java)
            .apply { setWidthFull(); value = element.packageMode }
        body.add(mode)
        return { element.packageMode = mode.value; true }
    }

    private fun getterChainField(initial: String): TextField =
        TextField(t("recording.naming.getterChain")).apply {
            setWidthFull()
            helperText = t("recording.naming.getterChain.helper")
            value = initial
        }

    private fun applyGetterChain(chain: dev.jvmguard.agent.config.base.CheckedString, field: TextField) {
        chain.value = field.value
        chain.isChecked = field.value.isNotBlank()
    }
}
