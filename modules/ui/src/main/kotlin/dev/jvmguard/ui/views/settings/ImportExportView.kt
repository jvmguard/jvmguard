package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.config.ImportManager
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.components.ErrorDialog
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.confirm
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.server.errorText
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
import com.vaadin.flow.component.upload.UploadI18N
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
class ImportExportView : AbstractSettingsPage() {

    init {
        add(exportSection(), importSection())
    }

    private fun exportSection() = settingsSection(
        t("common.export"),
        Span(t("settings.impex.export.hint")),
        exportAnchor(),
    )

    private fun exportAnchor(): Anchor = Anchor().apply {
        setHref(DownloadHandler.fromInputStream {
            val bytes = exportBytes()
            DownloadResponse(ByteArrayInputStream(bytes), ImportManager.SERVER_CONFIG_FILE_NAME, "application/json", bytes.size.toLong())
        })
        element.setAttribute("download", true)
        add(Button(t("settings.impex.export.button"), VaadinIcon.DOWNLOAD.create()).apply {
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
            setI18n(uploadI18n())
        }
        return settingsSection(
            t("common.import"),
            Span(t("settings.impex.import.hint")),
            upload,
        )
    }

    // TODO remove when https://github.com/vaadin/platform/issues/7581 is implemented
    private fun uploadI18n(): UploadI18N = UploadI18N().apply {
        addFiles = UploadI18N.AddFiles().setOne(t("impex.upload.addFile")).setMany(t("impex.upload.addFiles"))
        dropFiles = UploadI18N.DropFiles().setOne(t("impex.upload.dropFile")).setMany(t("impex.upload.dropFiles"))
        error = UploadI18N.Error()
            .setTooManyFiles(t("impex.upload.error.tooManyFiles"))
            .setFileIsTooBig(t("impex.upload.error.tooBig"))
            .setIncorrectFileType(t("impex.upload.error.wrongType"))
        uploading = UploadI18N.Uploading()
            .setStatus(
                UploadI18N.Uploading.Status()
                    .setConnecting(t("impex.upload.status.connecting"))
                    .setStalled(t("impex.upload.status.stalled"))
                    .setProcessing(t("impex.upload.status.processing"))
                    .setHeld(t("impex.upload.status.held")),
            )
            .setRemainingTime(
                UploadI18N.Uploading.RemainingTime()
                    .setPrefix(t("impex.upload.remainingPrefix"))
                    .setUnknown(t("impex.upload.remainingUnknown")),
            )
            .setError(
                UploadI18N.Uploading.Error()
                    .setServerUnavailable(t("impex.upload.error.unavailable"))
                    .setUnexpectedServerError(t("impex.upload.error.server"))
                    .setForbidden(t("impex.upload.error.forbidden")),
            )
        units = UploadI18N.Units().setSize(listOf("B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"))
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
        confirm(t("settings.impex.import.confirmTitle"), t("settings.impex.import.confirmText"), t("common.import")) {
            applyImport(bytes)
        }
    }

    private fun applyImport(bytes: ByteArray) {
        val connection = Sessions.current()?.serverConnection ?: return
        try {
            val config = connection.readConfig(bytes)
            if (config !is ServerInitConfig) {
                Notifications.show(t("settings.impex.import.invalidFile"))
                return
            }
            connection.applyInitConfig(config)
            UI.getCurrent().navigate(VmsView::class.java)
            Notifications.show(t("settings.impex.import.success"))
        } catch (e: CredentialException) {
            ErrorDialog(t("settings.impex.import.notPermitted"), errorText(e), null).open()
        } catch (e: Exception) {
            ErrorDialog(t("settings.impex.import.failed"), errorText(e), null).open()
        }
    }

    companion object {
        const val ID_EXPORT = "config-export"
        const val ID_UPLOAD = "config-upload"
    }
}
