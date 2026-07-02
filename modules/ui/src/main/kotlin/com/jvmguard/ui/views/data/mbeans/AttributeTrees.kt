package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.components.cellRow
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.provider.hierarchy.TreeData

const val ID_VALUE_DETAIL = "mbean-value-detail"

fun attributeValueSpan(node: AttributeNode): Span =
    Span(node.formattedValue()).apply {
        addClassName("jvmguard-mbean-value")
        if (node.isPlaceholder()) {
            addClassName("jvmguard-mbean-placeholder")
        }
    }

/** A "show entire value" button for a scalar string, or null when not applicable. Shown only while the
 *  value is actually clipped — wire it with [toggleOnOverflow]. */
fun valueDetailButton(node: AttributeNode): Button? {
    if (!node.isStringValue()) {
        return null
    }
    return Button("show") { ValueDetailDialog(node.value.toString()).open() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL)
        addClassName("jvmguard-field-icon-button")
        setAriaLabel("Show the entire value")
        setTooltipText("Show the entire value")
        testId = ID_VALUE_DETAIL
    }
}

/** Shows [button] only while [valueSpan] is truncated (its content exceeds the cell width). */
fun toggleOnOverflow(valueSpan: Span, button: Button) {
    button.style.set("display", "none")
    valueSpan.element.executeJs(
        $$"const s = this, b = $0;" +
                "const update = () => { b.style.display = s.scrollWidth > s.clientWidth + 1 ? '' : 'none'; };" +
                "requestAnimationFrame(update);" +
                "if (window.ResizeObserver) { new ResizeObserver(update).observe(s); }",
        button.element,
    )
}

/** The read-only value cell shared by the attribute tree and the operation-result tree: the value
 *  text plus a "show" affordance that appears only when the value overflows the cell. */
fun attributeValueCell(node: AttributeNode): Component {
    val span = attributeValueSpan(node)
    val detail = valueDetailButton(node) ?: return span
    return valueRow(span, detail).also { toggleOnOverflow(span, detail) }
}

internal fun valueRow(vararg components: Component): HorizontalLayout =
    cellRow(*components).apply {
        setWidthFull()
        setFlexGrow(1.0, components.first())
    }

fun addAttributeNodes(treeData: TreeData<AttributeNode>, parent: AttributeNode?, nodes: List<AttributeNode>) {
    for (node in nodes) {
        treeData.addItem(parent, node)
        addAttributeNodes(treeData, node, node.children)
    }
}
