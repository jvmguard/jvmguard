package com.jvmguard.ui.views.log

import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmsView
import com.jvmguard.connector.api.log.LogFile
import com.jvmguard.connector.api.log.LogFileDescriptor
import com.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.ClientCallable
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.server.streams.DownloadResponse
import com.vaadin.flow.shared.Registration
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

interface LogModeView {
    val logTitle: String
}

abstract class AbstractLogView(private val logFileType: LogFileType, viewTestId: String) :
    VerticalLayout(), BeforeEnterObserver, LogModeView {

    private data class LogLine(val sequence: Long, val text: String)

    override val logTitle: String get() = logFileType.toString()

    private val fileSelect = Select<LogFileDescriptor>().apply {
        label = null
        width = "20rem"
        testId = ID_FILE_SELECT
        setItemLabelGenerator { it.shortDescription }
        addValueChangeListener { openSelected() }
    }
    private val searchField = TextField().apply {
        placeholder = "Filter"
        isClearButtonVisible = true
        valueChangeMode = ValueChangeMode.LAZY
        testId = ID_SEARCH
        prefixComponent = VaadinIcon.SEARCH.create()
        addValueChangeListener {
            filter = it.value.orEmpty().trim()
            refreshGrid()
        }
    }
    private val autoScroll = Checkbox("Auto-scroll", true).apply {
        testId = ID_AUTO_SCROLL
        addValueChangeListener { if (it.value) scrollToEnd() }
    }
    private val downloadAnchor = Anchor().apply {
        element.setAttribute("download", true)
        isVisible = false
        add(Button("Download", VaadinIcon.DOWNLOAD.create()).apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            testId = ID_DOWNLOAD
        })
    }
    private val grid = Grid(LogLine::class.java, false).apply {
        addClassName("jvmguard-log-grid")
        addThemeVariants(GridVariant.NO_ROW_BORDERS, GridVariant.NO_BORDER)
        addColumn(LogLine::text).setKey(COLUMN_LINE)
        setSelectionMode(Grid.SelectionMode.NONE)
    }

    private val lines = ArrayList<LogLine>()
    private var nextSequence = 0L
    private var filter = ""
    private var logFile: LogFile? = null
    private var currentFileName: String? = null
    private var pollRegistration: Registration? = null
    private var scrollWired = false

    init {
        addClassName("jvmguard-log-view")
        testId = viewTestId
        setSizeFull()
        isPadding = true
        isSpacing = true

        val toolbar = HorizontalLayout(fileSelect, searchField, autoScroll, downloadAnchor).apply {
            addClassName("jvmguard-log-toolbar")
            setWidthFull()
            isWrap = true
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            expand(searchField)
        }
        add(toolbar, grid)
        expand(grid)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!Sessions.isLoggedIn()) {
            event.forwardTo(LoginView::class.java)
            return
        }
        if (Sessions.current()?.user?.accessLevel?.isAtLeast(logFileType.minimumAccessLevel) != true) {
            event.forwardTo(VmsView::class.java)
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        loadDescriptors()
        pollRegistration = attachEvent.ui.addPollListener { appendDelta() }
        wireUserScrollDetection()
    }

    // Turn auto-scroll off as soon as the user scrolls up user scroll away from the bottom does.
    private fun wireUserScrollDetection() {
        if (scrollWired) {
            return
        }
        scrollWired = true
        grid.element.executeJs(
            $$"""
                |const grid = this;
                |const view = $0;
                |customElements.whenDefined('vaadin-grid').then(() => {
                |    const scroller = grid.$ && grid.$.table;
                |    if (!scroller) return;
                |        scroller.addEventListener('scroll', () => {
                |            const away = scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight > 24;
                |            if (away) { 
                |                if (!grid.__autoScrollOff) { 
                |                    grid.__autoScrollOff = true; view.$server.userScrolledAway(); 
                |                } 
                |            } else { 
                |                grid.__autoScrollOff = false; 
                |            }
                |        });
                |    }
                |);""".trimMargin(),
            element,
        )
    }

    @ClientCallable
    fun userScrolledAway() {
        autoScroll.value = false
    }

    override fun onDetach(detachEvent: DetachEvent) {
        pollRegistration?.remove()
        pollRegistration = null
        closeLogFile()
        super.onDetach(detachEvent)
    }

    private fun loadDescriptors() {
        val descriptors = Sessions.current()?.serverConnection?.getLogFileDescriptors(logFileType).orEmpty()
        fileSelect.setItems(descriptors)
        fileSelect.value = descriptors.firstOrNull()
    }

    private fun openSelected() {
        val descriptor = fileSelect.value
        if (descriptor == null || descriptor.fileName == currentFileName) {
            return
        }
        closeLogFile()
        currentFileName = descriptor.fileName
        lines.clear()
        nextSequence = 0L
        logFile = Sessions.current()?.serverConnection?.getLogFile(descriptor.fileName)
        updateDownloadHandler()
        appendDelta()
    }

    private fun appendDelta() {
        val file = logFile ?: return
        val delta = file.componentDelta()
        if (delta.rotated) {
            lines.clear()
        }
        delta.lines.forEach { lines.add(LogLine(nextSequence++, it)) }
        trimToCapacity()
        refreshGrid()
    }

    private fun trimToCapacity() {
        if (lines.size > MAX_LINES) {
            lines.subList(0, lines.size - MAX_LINES).clear()
        }
    }

    private fun refreshGrid() {
        val visible = if (filter.isEmpty()) {
            lines
        } else {
            lines.filter { it.text.contains(filter, ignoreCase = true) }
        }
        grid.setItems(visible)
        if (autoScroll.value) {
            scrollToEnd()
        }
    }

    private fun scrollToEnd() {
        grid.scrollToEnd()
    }

    private fun updateDownloadHandler() {
        val fileName = currentFileName
        if (fileName == null) {
            downloadAnchor.isVisible = false
            downloadAnchor.removeHref()
            return
        }
        val downloadName = File(fileName).name
        downloadAnchor.setHref(DownloadHandler.fromInputStream { download(downloadName) })
        downloadAnchor.isVisible = true
    }

    private fun download(downloadName: String): DownloadResponse {
        val file = logFile?.file ?: return DownloadResponse.error(500)
        return DownloadResponse(BufferedInputStream(FileInputStream(file)), downloadName, "text/plain", file.length())
    }

    private fun closeLogFile() {
        logFile?.close()
        logFile = null
        currentFileName = null
    }

    companion object {
        const val ID_FILE_SELECT = "log-file-select"
        const val ID_SEARCH = "log-search"
        const val ID_AUTO_SCROLL = "log-auto-scroll"
        const val ID_DOWNLOAD = "log-download"
        private const val COLUMN_LINE = "line"
        private const val MAX_LINES = 5000
    }
}
