package dev.jvmguard.ui.components.recording.sets

import dev.jvmguard.agent.config.base.Identifiable
import dev.jvmguard.data.config.sets.AbstractSet
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant

class SetSpec<T : Identifiable, S : AbstractSet<T>>(
    val setClass: Class<S>,
    val addSubtitle: String,
    val saveSubtitle: String,
    val loadSets: () -> Collection<S>,
    val currentItems: () -> MutableList<T>,
    val createSet: (String, List<T>) -> S,
    val appendItems: (List<T>) -> Unit,
)

fun <T : Identifiable, S : AbstractSet<T>> setActionButtons(spec: SetSpec<T, S>): List<Component> {
    val add = Button(t("recording.set.add")) { AddSetDialog(spec).open() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_ADD
        setTooltipText(t("recording.set.add.tooltip"))
    }
    val save = Button(t("recording.set.save")) { SaveSetDialog(spec).open() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_SAVE
        isEnabled = spec.currentItems().isNotEmpty()
        setTooltipText(t("recording.set.save.tooltip"))
    }
    return listOf(add, save)
}

const val ID_ADD = "set-add"
const val ID_SAVE = "set-save"
