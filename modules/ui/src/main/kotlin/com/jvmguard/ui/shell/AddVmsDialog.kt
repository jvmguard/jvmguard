package com.jvmguard.ui.shell

import com.jvmguard.agent.comm.JvmGuardKeyManager
import com.jvmguard.data.agent.ArchiveFile
import com.jvmguard.data.agent.ArchiveFileType
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.Notifications
import com.jvmguard.ui.components.JvmGuardDialog
import com.jvmguard.ui.server.Sessions
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
        Notifications.show("You need at least \"${AccessLevel.PROFILER}\" access to add VMs.")
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
        headerTitle = "Add VMs"
        width = "34rem"
        add(Span("Is the VM that should be monitored running on this machine or on a remote machine?"))

        val cancel = Button("Cancel") { close() }
        val remote = Button("Remote machine") {
            close()
            AddRemoteVmsDialog().open()
        }.apply { testId = ID_REMOTE_MACHINE }
        val thisMachine = Button("This machine") {
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
        headerTitle = "Add locally running VMs"
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
            add(subtitle("Monitoring a VM is easy. Just add a VM parameter to your start script."))
            add(Span("Add the following VM parameter to the Java invocation in your start script:"))
            add(codeBlock(parameter))
            add(hint("Enhancement: to give the VM a proper name and group, append the following to the parameter above:"))
            add(codeBlock("${separator}name=$ISSUER_PLACEHOLDER,group=$GROUP_PLACEHOLDER"))
            add(hint("Advanced: for a pool of equivalent VMs (for example in a cloud environment), append this instead of name and group:"))
            add(codeBlock("${separator}pool=$POOL_PLACEHOLDER"))
            add(hint("Group and pool names can form a hierarchy such as level1/level2/level3."))
        })

        footer.add(Button("Close") { close() }.apply { testId = ID_CLOSE })
    }

    companion object {
        const val ID_CLOSE = "addvms-local-close"
    }
}

class AddRemoteVmsDialog : JvmGuardDialog() {

    @Volatile
    private var selectedType = ArchiveFileType.TAR_GZ

    init {
        headerTitle = "Add VMs"
        width = DIALOG_WIDTH

        val useSsl = Sessions.current()?.serverConnection?.let { runCatching { it.isUseSsl }.getOrDefault(false) } == true

        add(VerticalLayout().apply {
            isPadding = false
            isSpacing = true
            setWidthFull()
            add(subtitle("Monitoring a VM is easy. Just add a VM parameter to your start script. The agent files can be downloaded below."))
            add(step(1, "Download the agent", agentDownloadRow()))
            add(step(2, "Copy the agent to the machine where the VM is running", Span(copyInstruction(useSsl))))
            add(
                step(
                    3, "Add the VM parameter to the Java invocation in your start script",
                    codeBlock("-javaagent:[path to jvmguard.jar]=server=[IP address or name of the jvmguard server],name=$ISSUER_PLACEHOLDER,group=$GROUP_PLACEHOLDER"),
                    hint("Advanced: for a pool of equivalent VMs, append \",pool=$POOL_PLACEHOLDER\" instead of name and group. Group and pool names can form a hierarchy such as level1/level2/level3.")
                )
            )
        })

        footer.add(Button("Close") { close() }.apply { testId = ID_CLOSE })
    }

    private fun agentDownloadRow(): Component {
        val archiveType = EnumSelect("", ArchiveFileType::class.java) { it.toString() }.apply {
            label = null
            width = "14rem"
            value = selectedType
            testId = ID_ARCHIVE_TYPE
            addValueChangeListener { selectedType = it.value ?: ArchiveFileType.TAR_GZ }
        }
        val download = Anchor().apply {
            setHref(DownloadHandler.fromInputStream { agentDownload() })
            element.setAttribute("download", true)
            add(Button("Download", VaadinIcon.DOWNLOAD.create()).apply {
                addThemeVariants(ButtonVariant.PRIMARY)
                testId = ID_DOWNLOAD
            })
        }
        return HorizontalLayout(Span("Archive format"), archiveType, download).apply {
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
        "Communication with this jvmguard server is secured. The server certificate ${JvmGuardKeyManager.AGENT_STORE} is located next to jvmguard.jar."
    } else {
        "The location of the file jvmguard.jar is referenced in the next step."
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
    val label = Span("Step $number").apply { addClassName("jvmguard-step-label") }
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
        setAriaLabel("Copy to clipboard")
        setTooltipText("Copy")
    }
    copy.addClickListener {
        copy.element.executeJs($$"if (navigator.clipboard) { navigator.clipboard.writeText($0); }", text)
        Notifications.show("Copied to clipboard.")
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
