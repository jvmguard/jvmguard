package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.config.ImportManager
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.components.ErrorDialog
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.confirm
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.server.streams.DownloadResponse
import com.vaadin.flow.server.streams.UploadHandler
import jakarta.annotation.security.RolesAllowed
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.security.auth.login.CredentialException

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/import-export", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class ImportExportView : AbstractSettingsPage() {

    init {
        add(exportSection(), importSection())
    }

    private fun exportSection() = settingsSection(
        "Export",
        Span("The entire configuration is downloaded as a single file."),
        exportAnchor(),
    )

    private fun exportAnchor(): Anchor = Anchor().apply {
        setHref(DownloadHandler.fromInputStream {
            val bytes = exportBytes()
            DownloadResponse(ByteArrayInputStream(bytes), ImportManager.SERVER_CONFIG_FILE_NAME, "application/json", bytes.size.toLong())
        })
        element.setAttribute("download", true)
        add(Button("Export configuration", VaadinIcon.DOWNLOAD.create()).apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_EXPORT
        })
    }

    private fun importSection(): VerticalLayout {
        val upload = Upload(UploadHandler.inMemory { _, data -> onUploaded(data) }).apply {
            setAcceptedMimeTypes("application/json")
            setAcceptedFileExtensions(".json")
            setMaxFiles(1)
            isDropAllowed = true
            testId = ID_UPLOAD
        }
        return settingsSection(
            "Import",
            Span("Uploading a configuration file overwrites the entire server configuration."),
            upload,
        )
    }

    private fun onUploaded(data: ByteArray) {
        ui.ifPresent { it.access { confirmImport(data) } }
    }

    private fun exportBytes(): ByteArray = try {
        ByteArrayOutputStream().also { out ->
            Sessions.current()?.serverConnection?.serverInitConfig?.export(out)
        }.toByteArray()
    } catch (_: Exception) {
        ByteArray(0)
    }

    private fun confirmImport(bytes: ByteArray) {
        confirm("Import configuration", "This overwrites the entire server configuration. Continue?", "Import") {
            applyImport(bytes)
        }
    }

    private fun applyImport(bytes: ByteArray) {
        val connection = Sessions.current()?.serverConnection ?: return
        try {
            val config = connection.readConfig(bytes)
            if (config !is ServerInitConfig) {
                Notifications.show("The file is not a valid jvmguard configuration export.")
                return
            }
            connection.applyInitConfig(config)
            UI.getCurrent().navigate(VmsView::class.java)
            Notifications.show("Configuration imported.")
        } catch (e: CredentialException) {
            ErrorDialog("Import not permitted", e.message ?: e.toString(), null).open()
        } catch (e: Exception) {
            ErrorDialog("Could not import configuration", e.message ?: e.toString(), null).open()
        }
    }

    companion object {
        const val ID_EXPORT = "config-export"
        const val ID_UPLOAD = "config-upload"
    }
}
