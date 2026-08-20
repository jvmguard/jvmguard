package dev.jvmguard.ui.components.recording.sets

import dev.jvmguard.agent.config.base.Identifiable
import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.data.config.sets.AbstractSet
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class AddSetDialog<T : Identifiable, S : AbstractSet<T>>(private val spec: SetSpec<T, S>) : JvmGuardDialog() {

    init {
        headerTitle = t("recording.set.dialog.add")
        width = "34rem"
        isResizable = false

        val intro = Span(spec.addSubtitle).apply { addClassName("jvmguard-field-hint") }
        val sets = spec.loadSets().sortedBy { it.name }
        if (sets.isEmpty()) {
            add(VerticalLayout(intro, Span(t("recording.set.noneSaved"))).apply { isPadding = false })
            footer.add(Button(t("common.close")) { close() })
        } else {
            val grid = Grid<S>().apply {
                testId = ID_GRID
                addColumn { it.name }.setHeader(t("recording.name")).setFlexGrow(1)
                addColumn { it.items.size }.setHeader(t("recording.set.items")).setAutoWidth(true).setFlexGrow(0)
                setItems(sets)
                setWidthFull()
                height = "16rem"
                addItemDoubleClickListener { append(it.item) }
            }
            add(VerticalLayout(intro, grid).apply { isPadding = false; isSpacing = true })

            confirmFooter(t("common.add"), ID_CONFIRM) { grid.asSingleSelect().value?.let(::append) }
        }
    }

    private fun append(set: S) {
        val items = DeepCopy.clone(ArrayList(set.items)).onEach { it.resetModified() }
        spec.appendItems(items)
        close()
    }

    companion object {
        const val ID_GRID = "set-add-grid"
        const val ID_CONFIRM = "set-add-confirm"
    }
}
