package com.jvmguard.ui.views.settings.recording

import com.jvmguard.agent.config.base.OptionalConfig
import com.jvmguard.agent.config.recording.RetransformationType
import com.jvmguard.agent.config.transactions.TransactionDef
import com.jvmguard.agent.config.transactions.TransactionSettings
import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.sets.TransactionDefSet
import com.jvmguard.data.user.Roles
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.recording.TransactionDefGrid
import com.jvmguard.ui.components.recording.sets.SetSpec
import com.jvmguard.ui.components.recording.sets.setActionButtons
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/transactions", layout = MainLayout::class)
@PageTitle("jvmguard: Recording")
class RecordingTransactionsView : AbstractRecordingSettingsView() {

    override val overrideCategory: (GroupConfig) -> OptionalConfig get() = { it.transactionSettings }
    override val overrideLabel: String get() = "Override transaction settings for this group"

    private val retransformation = EnumSelect("Reinstrument classes", RetransformationType::class.java) { it.toString() }.apply {
        addClassName("jvmguard-settings-gap-before")
        addUserValueChangeListener { onRetransformationChanged() }
    }

    private var refreshToolbar: () -> Unit = {}

    override fun onSelectionChanged(selection: VmIdentifier) {
        val settings = transactionSettings(selection) ?: return
        retransformation.value = settings.retransformationType
        val declaredGrid = TransactionDefGrid(TransactionType.DECLARED, false, { settings.transactionDefs }, ::markChanged)
        val matchedGrid = TransactionDefGrid(TransactionType.MATCHED, true, { settings.transactionDefs }, ::markChanged)
        val mappedGrid = TransactionDefGrid(TransactionType.MAPPED, true, { settings.transactionDefs }, ::markChanged)
        val tabSheet = TabSheet().apply {
            setWidthFull()
            addClassName("jvmguard-recording-tabsheet")
            add("Declared", declaredGrid)
            add("Matched", matchedGrid)
            add("Mapped", mappedGrid)
        }
        content.removeAll()
        content.add(tabSheet, Span("Higher rows are matched first.").apply { addClassName("jvmguard-field-hint") }, retransformation)

        refreshToolbar = {
            val add = Button("Add transaction", VaadinIcon.PLUS.create()) {
                when (tabSheet.selectedIndex) {
                    0 -> declaredGrid
                    2 -> mappedGrid
                    else -> matchedGrid
                }.addDef()
            }.apply {
                addThemeVariants(ButtonVariant.PRIMARY)
                testId = "transaction-add"
            }
            setToolbarActions(add, *setActionButtons(setSpec(settings, declaredGrid, matchedGrid, mappedGrid)).toTypedArray())
        }
        refreshToolbar()
    }

    private fun setSpec(
        settings: TransactionSettings,
        declaredGrid: TransactionDefGrid,
        matchedGrid: TransactionDefGrid,
        mappedGrid: TransactionDefGrid,
    ): SetSpec<TransactionDef, TransactionDefSet> =
        SetSpec(
            setClass = TransactionDefSet::class.java,
            singularName = "transaction set",
            pluralName = "transaction sets",
            addSubtitle = "The business transactions in the selected set are added to this group.",
            saveSubtitle = "Saved transaction sets can be added to the transactions of other groups.",
            loadSets = {
                Sessions.current()?.serverConnection?.transactionDefSets ?: emptyList()
            },
            currentItems = { settings.transactionDefs },
            createSet = { name, items -> TransactionDefSet(name, items) },
            appendItems = { items ->
                settings.transactionDefs.addAll(items)
                markChanged()
                matchedGrid.refresh()
                declaredGrid.refresh()
                mappedGrid.refresh()
            },
        )

    override fun onEditableChanged(editable: Boolean) {
        content.isEnabled = editable
    }

    private fun onRetransformationChanged() {
        transactionSettings(Sessions.recordingGroupSelection().selection)?.retransformationType = retransformation.value
        markChanged()
    }

    private fun markChanged() {
        Sessions.recordingDraft().markChanged(Sessions.recordingGroupSelection().selection)
        refreshToolbar()
    }

    private fun transactionSettings(selection: VmIdentifier): TransactionSettings? =
        Sessions.recordingDraft().groupConfig(selection)?.transactionSettings
}
