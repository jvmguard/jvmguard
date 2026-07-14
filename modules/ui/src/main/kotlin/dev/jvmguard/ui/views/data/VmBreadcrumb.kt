package dev.jvmguard.ui.views.data

import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.textLink
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

/**
 * A breadcrumb of the selected group/JVM hierarchy the leaf is the current selection.
 */
class VmBreadcrumb(private val onSelect: (VmIdentifier) -> Unit) : HorizontalLayout() {

    init {
        addClassName("jvmguard-breadcrumb")
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
        isSpacing = false
        isWrap = true
    }

    fun setSelection(selection: VmIdentifier) {
        removeAll()
        val chain = chain(selection)
        chain.forEachIndexed { index, identifier ->
            if (index > 0) {
                add(Span("/").apply { addClassName("jvmguard-breadcrumb-separator") })
            }
            val label = displayName(identifier)
            if (index == chain.lastIndex) {
                add(Span(label).apply { addClassName("jvmguard-breadcrumb-current") })
            } else {
                add(textLink(label) { onSelect(identifier) })
            }
        }
    }

    companion object {

        private fun chain(selection: VmIdentifier): List<VmIdentifier> {
            val chain = ArrayDeque<VmIdentifier>()
            var current: VmIdentifier? = selection
            while (current != null) {
                chain.addFirst(current)
                current = current.parent
            }
            return chain
        }

        private fun displayName(identifier: VmIdentifier): String =
            if (identifier.isRoot) "All JVMs" else identifier.toUnqualified().name
    }
}
