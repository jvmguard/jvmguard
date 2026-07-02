package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.NamingElement
import com.jvmguard.agent.config.transactions.naming.*
import com.jvmguard.ui.components.*
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
        addColumn { it.displayName }.setHeader("Naming element").setFlexGrow(1)
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
        val hint = Span("The transaction name is built from these elements, in order.")
        val addButton = menuButton(VaadinIcon.PLUS, "Add naming element", ID_ADD) {
            ELEMENT_TYPES.forEach { type -> addItem(type.label) { addElement(type) } }
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
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", "$ID_ROW_MENU-${elements.indexOf(element)}") {
            if (hasDialog(element)) {
                addItem("Edit") { edit(element) }
            }
            addItem("Remove") { remove(element) }
        }

    private fun remove(element: NamingElement) {
        elements.remove(element)
        refresh()
    }

    private fun refresh() {
        grid.setItems(elements)
    }

    private enum class ElementType(val label: String, val create: () -> NamingElement, val hasDialog: Boolean) {
        CLASS_NAME("Class name", { ClassNameElement() }, true),
        INSTANCE_CLASS_NAME("Instance class name", { InstanceClassNameElement() }, true),
        INSTANCE("Instance name", { InstanceElement() }, true),
        METHOD_PARAMETER("Method parameter", { MethodParameterElement() }, true),
        METHOD_NAME("Method name", { MethodNameElement() }, false),
        TEXT("Fixed text", { TextElement() }, true),
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
        headerTitle = "Naming element"
        width = "34rem"

        val body = VerticalLayout().apply { isPadding = false; isSpacing = true }
        val apply: () -> Boolean = buildFields(body)
        add(body)

        val cancel = Button("Cancel") { close() }
        val save = Button("Save") {
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
                val text = TextField("Text").apply { setWidthFull(); value = e.text }
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
                val index = IntegerField("Parameter index (0-based)").apply { width = "12rem"; value = e.parameterIndex }
                val getter = getterChainField(e.getterChain.usedValue)
                body.add(index, getter)
                return { e.parameterIndex = index.value ?: 0; applyGetterChain(e.getterChain, getter); true }
            }

            else -> return { true }
        }
    }

    private fun packageModeField(body: VerticalLayout, element: ClassNameElement): () -> Boolean {
        val mode = EnumSelect("Package name", ClassNameElement.PackageMode::class.java) { it.toString() }
            .apply { setWidthFull(); value = element.packageMode }
        body.add(mode)
        return { element.packageMode = mode.value; true }
    }

    private fun getterChainField(initial: String): TextField =
        TextField("Getter chain (optional)").apply {
            setWidthFull()
            helperText = "Dot-separated getters applied before converting to a string, e.g. getId.name"
            value = initial
        }

    private fun applyGetterChain(chain: com.jvmguard.agent.config.base.CheckedString, field: TextField) {
        chain.value = field.value
        chain.isChecked = field.value.isNotBlank()
    }
}
