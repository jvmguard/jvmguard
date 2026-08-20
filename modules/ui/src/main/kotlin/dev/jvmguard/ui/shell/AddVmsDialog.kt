package dev.jvmguard.ui.shell

import dev.jvmguard.agent.comm.JvmGuardKeyManager
import dev.jvmguard.data.agent.ArchiveFile
import dev.jvmguard.data.agent.ArchiveFileType
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.server.streams.DownloadResponse
import java.io.File
import java.io.FileInputStream

private const val DIALOG_WIDTH = "44rem"
private const val ISSUER_PLACEHOLDER = "[a name for the VM]"
private const val GROUP_PLACEHOLDER = "[an optional group name for the VM]"
private const val POOL_PLACEHOLDER = "[a name for the VM pool]"

fun openAddVms() {
    val session = Sessions.current() ?: return
    if (!session.user.accessLevel.isAtLeast(AccessLevel.PROFILER)) {
        Notifications.show(t("shell.addVms.accessRequired", enumLabel(AccessLevel.PROFILER)))
        return
    }
    if (session.isLocalRequest()) {
        AddVmsLocationDialog().open()
    } else {
        AddRemoteVmsDialog().open()
    }
}

class AddVmsLocationDialog : JvmGuardDialog() {

    init {
        headerTitle = t("shell.addVms")
        width = "34rem"
        add(Span(t("shell.addVms.location.question")))

        val cancel = Button(t("common.cancel")) { close() }
        val remote = Button(t("shell.addVms.location.remote")) {
            close()
            AddRemoteVmsDialog().open()
        }.apply { testId = ID_REMOTE_MACHINE }
        val thisMachine = Button(t("shell.addVms.location.thisMachine")) {
            close()
            AddLocalVmsDialog().open()
        }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_THIS_MACHINE
        }
        footer.add(cancel, remote, thisMachine)
    }

    companion object {
        const val ID_THIS_MACHINE = "addvms-this-machine"
        const val ID_REMOTE_MACHINE = "addvms-remote-machine"
    }
}

class AddLocalVmsDialog : JvmGuardDialog() {

    init {
        headerTitle = t("shell.addVms.local.title")
        width = DIALOG_WIDTH

        val connection = Sessions.current()?.serverConnection
        val agentPath = orUnknown { connection?.agentPath }
        val useSsl = connection != null && runCatching { connection.isUseSsl }.getOrDefault(false)
        val dataDirectory = runCatching { connection?.dataDirectory }.getOrNull()

        val parameter = buildString {
            append("-javaagent:").append(agentPath)
            if (useSsl && dataDirectory != null) {
                append("=keyStore=").append(File(dataDirectory, "ssl/${JvmGuardKeyManager.AGENT_STORE}").path)
            }
        }
        val separator = if (useSsl) "," else "="

        add(VerticalLayout().apply {
            isPadding = false
            isSpacing = true
            setWidthFull()
            add(subtitle(t("shell.addVms.local.intro")))
            add(Span(t("shell.addVms.local.jvmOption")))
            add(codeBlock(parameter))
            add(hint(t("shell.addVms.local.nameGroup")))
            add(codeBlock("${separator}name=$ISSUER_PLACEHOLDER,group=$GROUP_PLACEHOLDER"))
            add(hint(t("shell.addVms.local.pool")))
            add(codeBlock("${separator}pool=$POOL_PLACEHOLDER"))
            add(hint(t("shell.addVms.hierarchy")))
        })

        footer.add(Button(t("common.close")) { close() }.apply { testId = ID_CLOSE })
    }

    companion object {
        const val ID_CLOSE = "addvms-local-close"
    }
}

class AddRemoteVmsDialog : JvmGuardDialog() {

    @Volatile
    private var selectedType = ArchiveFileType.TAR_GZ

    init {
        headerTitle = t("shell.addVms")
        width = DIALOG_WIDTH

        val useSsl = Sessions.current()?.serverConnection?.let { runCatching { it.isUseSsl }.getOrDefault(false) } == true

        add(VerticalLayout().apply {
            isPadding = false
            isSpacing = true
            setWidthFull()
            add(subtitle(t("shell.addVms.remote.intro")))
            add(step(1, t("shell.addVms.remote.stepDownload"), agentDownloadRow()))
            add(step(2, t("shell.addVms.remote.stepCopy"), Span(copyInstruction(useSsl))))
            add(
                step(
                    3, t("shell.addVms.remote.stepJvmOption"),
                    codeBlock("-javaagent:[path to jvmguard.jar]=server=[IP address or name of the jvmguard server],name=$ISSUER_PLACEHOLDER,group=$GROUP_PLACEHOLDER"),
                    hint(t("shell.addVms.remote.pool", POOL_PLACEHOLDER))
                )
            )
        })

        footer.add(Button(t("common.close")) { close() }.apply { testId = ID_CLOSE })
    }

    private fun agentDownloadRow(): Component {
        val archiveType = EnumSelect("", ArchiveFileType::class.java) { enumLabel(it) }.apply {
            label = null
            width = "14rem"
            value = selectedType
            testId = ID_ARCHIVE_TYPE
            addValueChangeListener { selectedType = it.value ?: ArchiveFileType.TAR_GZ }
        }
        val download = Anchor().apply {
            setHref(DownloadHandler.fromInputStream { agentDownload() })
            element.setAttribute("download", true)
            add(Button(t("common.download"), VaadinIcon.DOWNLOAD.create()).apply {
                addThemeVariants(ButtonVariant.PRIMARY)
                testId = ID_DOWNLOAD
            })
        }
        return HorizontalLayout(Span(t("shell.addVms.remote.archiveFormat")), archiveType, download).apply {
            addClassName("jvmguard-addvms-download-row")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
        }
    }

    private fun agentDownload(): DownloadResponse {
        val connection = Sessions.current()?.serverConnection ?: return DownloadResponse.error(500)
        val type = selectedType
        return try {
            val archive: ArchiveFile = connection.getAgentArchiveFile(type)
            DownloadResponse(FileInputStream(archive.file), type.fileName, "application/octet-stream", archive.fileSize)
        } catch (_: Exception) {
            DownloadResponse.error(500)
        }
    }

    private fun copyInstruction(useSsl: Boolean): String = if (useSsl) {
        t("shell.addVms.remote.copySsl", JvmGuardKeyManager.AGENT_STORE)
    } else {
        t("shell.addVms.remote.copyPlain")
    }

    companion object {
        const val ID_ARCHIVE_TYPE = "addvms-archive-type"
        const val ID_DOWNLOAD = "addvms-download"
        const val ID_CLOSE = "addvms-remote-close"
    }
}

private fun subtitle(text: String): Component = Span(text).apply { addClassName("jvmguard-dialog-subtitle") }

private fun hint(text: String): Component = Span(text).apply { addClassName("jvmguard-field-hint") }

private fun step(number: Int, title: String, vararg content: Component): Component {
    val body = VerticalLayout(Span(title).apply { addClassName("jvmguard-step-title") }, *content).apply {
        isPadding = false
        isSpacing = true
        setWidthFull()
    }
    val label = Span(t("shell.addVms.step", number)).apply { addClassName("jvmguard-step-label") }
    return HorizontalLayout(label, body).apply {
        addClassName("jvmguard-step")
        setWidthFull()
        isPadding = false
        expand(body)
    }
}

private fun codeBlock(text: String): Component {
    val code = Span(text).apply { addClassName("jvmguard-code-text") }
    val copy = Button(VaadinIcon.COPY_O.create()).apply {
        addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL)
        setAriaLabel(t("common.copyToClipboard"))
        setTooltipText(t("common.copy"))
    }
    copy.addClickListener {
        copy.element.executeJs($$"if (navigator.clipboard) { navigator.clipboard.writeText($0); }", text)
        Notifications.show(t("common.copiedToClipboard"))
    }
    return HorizontalLayout(code, copy).apply {
        addClassName("jvmguard-code-block")
        setWidthFull()
        defaultVerticalComponentAlignment = FlexComponent.Alignment.START
        expand(code)
        isPadding = false
    }
}

private inline fun orUnknown(block: () -> String?): String = runCatching { block() }.getOrNull().orEmpty().ifEmpty { "<unknown>" }
