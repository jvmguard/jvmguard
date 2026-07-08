package com.jvmguard.ui.views.inbox

import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.notification.ModificationType
import com.jvmguard.data.user.InboxItem
import com.jvmguard.ui.components.Formats
import com.jvmguard.ui.components.confirm
import com.jvmguard.ui.components.menuButton
import com.jvmguard.ui.server.ModificationListener
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.registerModificationListener
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.login.LoginView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.server.streams.DownloadResponse
import jakarta.annotation.security.PermitAll
import java.io.BufferedInputStream
import java.io.FileInputStream

@PermitAll
@Route(value = "inbox", layout = MainLayout::class)
@PageTitle("jvmguard: Inbox")
class InboxView : VerticalLayout(), BeforeEnterObserver, ModificationListener {

    private val items = mutableListOf<InboxItem>()
    private var focusedItem: InboxItem? = null
    private var focusedColumnKey: String? = null
    private var multiMode = false

    private val deleteMultipleButton = Button("Delete multiple", VaadinIcon.TRASH.create()) {
        if (multiMode) {
            confirmDeleteSelected()
        } else {
            enterMultiMode()
        }
    }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_DELETE_MULTIPLE
    }
    private val cancelMultiButton = Button("Cancel") { exitMultiMode() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_CANCEL_MULTI
        isVisible = false
    }

    // Hidden anchor used to fire a download from a keyboard/double-click action, which are server events
    // and cannot otherwise start a browser download. Its href is set to the activated item, then clicked.
    private val activationDownload = Anchor().apply {
        element.setAttribute("download", true)
        style.set("display", "none")
    }

    private val grid = Grid(InboxItem::class.java, false).apply {
        testId = ID_GRID
        addClassName("jvmguard-inbox-grid")
        addThemeVariants(GridVariant.NO_BORDER)
        addComponentColumn { textCell(Formats.dateTime(it.date), it) }.setHeader("Date").setAutoWidth(true).setFlexGrow(0)
        addComponentColumn { textCell(typeLabel(it), it) }.setHeader("Type").setAutoWidth(true).setFlexGrow(0)
        addComponentColumn { nameCell(it) }.setHeader("Name").setKey(COL_NAME).setFlexGrow(1)
        addItemDoubleClickListener { primaryAction(it.item) }
        addCellFocusListener {
            focusedItem = it.item.orElse(null)
            focusedColumnKey = it.column.orElse(null)?.key
        }
        // Enter runs the primary row action, but not on the name cell because that cell holds the actions menu
        element.addEventListener("keydown") {
            if (focusedColumnKey != COL_NAME) {
                focusedItem?.let(::primaryAction)
            }
        }.filter = "event.key === 'Enter' && event.target === event.currentTarget"
        element.addEventListener("keydown") { confirmDelete(deleteTargets()) }
            .setFilter("event.key === 'Delete' && event.target === event.currentTarget")
            .addEventData("event.preventDefault()")
        setSizeFull()
    }

    init {
        addClassName("jvmguard-inbox-view")
        setSizeFull()
        isPadding = true
        isSpacing = true

        val toolbar = HorizontalLayout(deleteMultipleButton, cancelMultiButton).apply {
            addClassName("jvmguard-inbox-toolbar")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
        }
        add(toolbar, grid, activationDownload)
        expand(grid)
        applySelectionMode(Grid.SelectionMode.SINGLE)
    }

    // the listener must be re-attached on every mode change.
    private fun applySelectionMode(mode: Grid.SelectionMode) {
        grid.setSelectionMode(mode)
        grid.addSelectionListener { onSelectionChanged() }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!Sessions.isLoggedIn()) {
            event.forwardTo(LoginView::class.java)
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        val session = Sessions.current() ?: return
        registerModificationListener(session)
        reload()
    }

    override fun modifyNotified(modificationTypes: Set<ModificationType>) {
        if (ModificationType.INBOX in modificationTypes) {
            reload()
        }
    }

    private fun reload() {
        items.clear()
        Sessions.current()?.serverConnection?.inboxItems
            ?.sortedByDescending { it.date }
            ?.let { items.addAll(it) }
        grid.setItems(items)
        syncBadge()
    }

    private fun textCell(text: String, item: InboxItem): Span =
        Span(text).apply { if (!item.isItemRead) addClassName(UNREAD_CLASS) }

    private fun nameCell(item: InboxItem): Component {
        val name = textCell(item.name, item)
        val menu = menuButton(
            VaadinIcon.ELLIPSIS_DOTS_V,
            "Actions for ${item.name}",
            "$ID_ROW_MENU-${item.id}",
        ) {
            if (item.message.isNotBlank()) {
                addItem("View message") { viewMessage(item) }
            }
            if (item.snapshotFileId != null) {
                addItem("Download") { startDownload(item) }
            }
            addItem("Delete") { confirmDelete(listOf(item)) }
        }
        return HorizontalLayout(name, menu).apply {
            setWidthFull()
            isPadding = false
            isSpacing = false
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            expand(name)
        }
    }

    private fun primaryAction(item: InboxItem) {
        when {
            item.message.isNotBlank() -> viewMessage(item)
            item.snapshotFileId != null -> startDownload(item)
        }
    }

    private fun onSelectionChanged() {
        if (multiMode) {
            updateMultiButton()
        } else {
            markSelectedRead()
        }
    }

    private fun markSelectedRead() {
        val newlyRead = grid.selectedItems.filter { !it.isItemRead }
        if (newlyRead.isEmpty()) {
            return
        }
        newlyRead.forEach {
            it.isItemRead = true
            grid.dataProvider.refreshItem(it)
        }
        syncBadge()
        modify(modified = newlyRead.toList())
    }

    private fun enterMultiMode() {
        multiMode = true
        applySelectionMode(Grid.SelectionMode.MULTI)
        cancelMultiButton.isVisible = true
        updateMultiButton()
    }

    private fun exitMultiMode() {
        multiMode = false
        applySelectionMode(Grid.SelectionMode.SINGLE)
        cancelMultiButton.isVisible = false
        deleteMultipleButton.text = "Delete multiple"
        deleteMultipleButton.isEnabled = true
    }

    private fun updateMultiButton() {
        val count = grid.selectedItems.size
        deleteMultipleButton.text = if (count == 0) "Delete selected" else "Delete selected ($count)"
        deleteMultipleButton.isEnabled = count > 0
    }

    private fun confirmDeleteSelected() {
        confirmDelete(grid.selectedItems.toList()) { exitMultiMode() }
    }

    private fun deleteTargets(): List<InboxItem> =
        grid.selectedItems.ifEmpty { setOfNotNull(focusedItem) }.toList()

    private fun confirmDelete(targets: List<InboxItem>, onDeleted: () -> Unit = {}) {
        if (targets.isEmpty()) {
            return
        }
        val header = if (targets.size == 1) "Delete message" else "Delete messages"
        val text = if (targets.size == 1) "Really delete this message?" else "Really delete ${targets.size} messages?"
        confirm(header, text, "Delete") {
            performDelete(targets)
            onDeleted()
        }
    }

    private fun performDelete(targets: List<InboxItem>) {
        items.removeAll(targets.toSet())
        grid.setItems(items)
        grid.deselectAll()
        syncBadge()
        modify(removed = targets)
    }

    private fun syncBadge() {
        findAncestor(MainLayout::class.java)?.showInboxUnread(items.count { !it.isItemRead })
    }

    private fun viewMessage(item: InboxItem) {
        InboxMessageDialog(item.name, item.message).open()
    }

    private fun startDownload(item: InboxItem) {
        val snapshotFileId = item.snapshotFileId ?: return
        activationDownload.setHref(DownloadHandler.fromInputStream { download(snapshotFileId) })
        activationDownload.element.callJsFunction("click")
    }

    private fun download(snapshotFileId: Long): DownloadResponse {
        return try {
            val snapshotFile = Sessions.current()?.serverConnection?.getSnapshotFile(snapshotFileId)
                ?: return DownloadResponse.error(500)
            val file = snapshotFile.file
            DownloadResponse(
                BufferedInputStream(FileInputStream(file)),
                snapshotFile.targetFileName,
                "application/octet-stream",
                file.length(),
            )
        } catch (_: Exception) {
            DownloadResponse.error(500)
        }
    }

    private fun modify(modified: List<InboxItem> = emptyList(), removed: List<InboxItem> = emptyList()) {
        Sessions.current()?.serverConnection?.modifyInboxItems(
            ListModification(modified, removed, emptyList(), InboxItem::class.java),
        )
    }

    companion object {
        const val ID_GRID = "inbox-grid"
        const val ID_ROW_MENU = "inbox-row-menu"
        const val ID_DELETE_MULTIPLE = "inbox-delete-multiple"
        const val ID_CANCEL_MULTI = "inbox-cancel-multi"
        const val COL_NAME = "name"
        const val UNREAD_CLASS = "jvmguard-unread"

        private fun typeLabel(item: InboxItem): String = item.snapshotFileType?.toString() ?: "Message"
    }
}
