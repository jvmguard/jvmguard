package com.jvmguard.ui.components.recording.triggers

import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.actions.*
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.JvmGuardDialog
import com.jvmguard.ui.components.Validators
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import tools.jackson.databind.json.JsonMapper
import java.net.URI

// the enclosing trigger is a deep copy in TriggerDialog, so Cancel discards everything.
abstract class TriggerActionDialog protected constructor(
    title: String,
    private val onSave: () -> Unit,
) : JvmGuardDialog() {

    protected val body = VerticalLayout().apply { isPadding = false; isSpacing = true }

    init {
        headerTitle = title
        width = "46rem"
        isResizable = false
    }

    // Subclasses call this at the end of their constructor; a base init block runs before subclass fields exist.
    protected fun build() {
        populate()
        add(body)
        confirmFooter("Save", ID_SAVE) {
            if (writeBack()) {
                onSave()
                close()
            }
        }
    }

    protected abstract fun populate()

    protected abstract fun writeBack(): Boolean

    companion object {
        const val ID_SAVE = "trigger-action-save"

        fun create(action: TriggerAction, isNew: Boolean, onSave: () -> Unit): TriggerActionDialog =
            create(action, (if (isNew) "Add action: " else "Edit action: ") + action.actionType, onSave)

        fun create(action: TriggerAction, title: String, onSave: () -> Unit): TriggerActionDialog =
            when (action) {
                is RecordJpsAction -> RecordJpsActionDialog(action, title, onSave)
                is RecordJfrAction -> RecordJfrActionDialog(action, title, onSave)
                is ThreadDumpAction -> SnapshotActionDialog(action, title, onSave)
                is HeapDumpAction -> SnapshotActionDialog(action, title, onSave)
                is EmailAction -> EmailActionDialog(action, title, onSave)
                is WebhookAction -> WebhookActionDialog(action, title, onSave)
                is LogAction -> LogActionDialog(action, title, onSave)
                is InboxAction -> InboxActionDialog(action, title, onSave)
                else -> throw IllegalArgumentException("No editor for action type ${action.actionType}")
            }
    }
}

private abstract class ArtifactActionDialog protected constructor(
    private val action: ArtifactAction,
    title: String,
    onSave: () -> Unit,
) : TriggerActionDialog(title, onSave) {

    protected val artifact = artifactNameField(action.artifactName)
    protected val inbox = inboxCheckbox(action.isCreateInboxItem)

    protected fun writeArtifact() {
        action.artifactName = artifact.value
        action.isCreateInboxItem = inbox.value
    }
}

private class SnapshotActionDialog(action: ArtifactAction, title: String, onSave: () -> Unit) :
    ArtifactActionDialog(action, title, onSave) {

    init {
        build()
    }

    override fun populate() {
        body.add(artifact, inbox)
    }

    override fun writeBack(): Boolean {
        if (!requireArtifactName(artifact)) {
            return false
        }
        writeArtifact()
        return true
    }
}

private abstract class RecordArtifactActionDialog protected constructor(
    private val action: RecordArtifactAction,
    title: String,
    onSave: () -> Unit,
) : ArtifactActionDialog(action, title, onSave) {

    protected val time = IntegerField("Recording time").apply { width = "8rem"; value = action.time }
    protected val unit = EnumSelect("", TimeUnit::class.java) { it.toString() }.apply { value = action.timeUnit }

    protected fun validateRecordArtifact(): Boolean = requireArtifactName(artifact) && requireRecordingTime(time)

    protected fun writeRecordArtifact() {
        writeArtifact()
        action.time = time.value ?: 1
        action.timeUnit = unit.value
    }
}

private class RecordJpsActionDialog(private val action: RecordJpsAction, title: String, onSave: () -> Unit) :
    RecordArtifactActionDialog(action, title, onSave) {

    private val unknownSubsystems = action.subsystems.filter { JProfilerSubsystem.fromId(it) == null }.toSet()

    private val subsystems = MultiSelectComboBox<JProfilerSubsystem>("Recorded subsystems").apply {
        setItems(*JProfilerSubsystem.entries.toTypedArray())
        setItemLabelGenerator { it.label }
        setWidthFull()
        value = action.subsystems.mapNotNull { JProfilerSubsystem.fromId(it) }.toSet()
        addClassName("jvmguard-settings-gap-before")
    }

    // Point-in-time captures taken at the end of the recording window and stored in the same snapshot.
    private val heapDump = Checkbox("Include heap dump").apply {
        value = action.heapDump
        addClassName("jvmguard-settings-gap-before")
    }

    private val heapDumpFullGc = Checkbox("Run a full GC before the heap dump").apply {
        value = action.heapDumpFullGc
        isEnabled = action.heapDump
    }

    private val mbeanSnapshot = Checkbox("Include MBean snapshot").apply { value = action.mbeanSnapshot }

    private val monitorDump = Checkbox("Include monitor dump").apply { value = action.monitorDump }

    init {
        heapDump.addValueChangeListener { heapDumpFullGc.isEnabled = it.value }
        build()
    }

    override fun populate() {
        body.add(artifact, timeRow(time, unit), inbox, subsystems, heapDump, heapDumpFullGc, mbeanSnapshot, monitorDump)
    }

    override fun writeBack(): Boolean {
        if (!validateRecordArtifact()) {
            return false
        }
        if (subsystems.value.isEmpty()) {
            return fail(subsystems, "Select at least one subsystem.")
        }
        writeRecordArtifact()
        action.subsystems = subsystems.value.map { it.id }.toSet() + unknownSubsystems
        action.heapDump = heapDump.value
        action.heapDumpFullGc = heapDumpFullGc.value
        action.mbeanSnapshot = mbeanSnapshot.value
        action.monitorDump = monitorDump.value
        return true
    }
}

private class RecordJfrActionDialog(private val action: RecordJfrAction, title: String, onSave: () -> Unit) :
    RecordArtifactActionDialog(action, title, onSave) {

    private val mode = RadioButtonGroup<JfrConfigMode>().apply {
        setItems(*JfrConfigMode.entries.toTypedArray())
        setItemLabelGenerator { it.toString() }
        value = action.configMode
        addClassName("jvmguard-settings-gap-before")
    }
    private val profile = TextField("Profile name").apply {
        setWidthFull()
        helperText = "Predefined profiles: " + JfrDefaultProfile.entries.joinToString(", ") { it.toString() }
        value = action.profileName
    }
    private val settings = TextArea("JFR settings").apply { setWidthFull(); value = action.settings }

    init {
        mode.addValueChangeListener { updateMode() }
        updateMode()
        build()
    }

    override fun populate() {
        body.add(artifact, timeRow(time, unit), inbox, mode, profile, settings)
    }

    override fun writeBack(): Boolean {
        if (!validateRecordArtifact()) {
            return false
        }
        if (mode.value == JfrConfigMode.CONFIG_FILE && settings.value.isBlank()) {
            return fail(settings, "Please enter JFR settings.")
        }
        writeRecordArtifact()
        action.configMode = mode.value
        action.profileName = profile.value.orEmpty()
        action.settings = settings.value
        return true
    }

    private fun updateMode() {
        profile.isVisible = mode.value == JfrConfigMode.PREDEFINED
        settings.isVisible = mode.value == JfrConfigMode.CONFIG_FILE
    }
}

private abstract class TextActionDialog protected constructor(
    private val action: TextAction,
    title: String,
    onSave: () -> Unit,
) : TriggerActionDialog(title, onSave) {

    protected val text = textAreaField(action.text)

    protected fun writeText() {
        action.text = text.value
    }
}

private class InboxActionDialog(action: InboxAction, title: String, onSave: () -> Unit) :
    TextActionDialog(action, title, onSave) {

    init {
        build()
    }

    override fun populate() {
        body.add(text)
    }

    override fun writeBack(): Boolean {
        if (!requireText(text)) {
            return false
        }
        writeText()
        return true
    }
}

private class LogActionDialog(private val action: LogAction, title: String, onSave: () -> Unit) :
    TextActionDialog(action, title, onSave) {

    private val category = EnumSelect("Category", LogCategory::class.java) { it.toString() }.apply { value = action.category }

    init {
        build()
    }

    override fun populate() {
        body.add(category, text)
    }

    override fun writeBack(): Boolean {
        if (!requireText(text)) {
            return false
        }
        action.category = category.value
        writeText()
        return true
    }
}

private class EmailActionDialog(private val action: EmailAction, title: String, onSave: () -> Unit) :
    TextActionDialog(action, title, onSave) {

    private val email = TextField("Email").apply { setWidthFull(); value = action.email }
    private val category = TextField("Category").apply {
        setWidthFull()
        helperText = "e.g. " + LogCategory.entries.joinToString(", ") { it.toString() }
        value = action.category
    }

    init {
        build()
    }

    override fun populate() {
        body.add(email, category, text)
    }

    override fun writeBack(): Boolean {
        if (email.value.isNullOrBlank()) {
            return fail(email, "Please enter an email address.")
        }
        if (!Validators.isValidEmail(email.value)) {
            return fail(email, "The email address is invalid.")
        }
        if (!requireText(text)) {
            return false
        }
        action.email = email.value
        action.category = category.value.orEmpty()
        writeText()
        return true
    }
}

private class WebhookActionDialog(private val action: WebhookAction, title: String, onSave: () -> Unit) :
    TriggerActionDialog(title, onSave) {

    private val url = TextField("URL").apply { setWidthFull(); value = action.url }
    private val method = EnumSelect("Method", HttpRequestMethod::class.java) { it.toString() }.apply { value = action.httpRequestMethod }
    private val headers = TextArea("Headers").apply {
        setWidthFull()
        helperText = "One header per line, as key=value. $TRIGGER_HELP"
        value = action.headers
    }
    private val bodyType = EnumSelect("Body", BodyContentType::class.java) { it.toString() }.apply { value = action.bodyContentType }
    private val formData = TextArea("Form data").apply {
        setWidthFull()
        helperText = "Unencoded parameters as key=value, one per line. $TRIGGER_HELP"
        value = action.formData
    }
    private val json = TextArea("JSON").apply {
        setWidthFull()
        helperText = "A JSON string sent in the request body. $TRIGGER_HELP"
        value = action.json
    }
    private val timeout = IntegerField("Timeout (seconds)").apply { width = "10rem"; value = action.timeout }
    private val ssl = Checkbox("Accept all SSL certificates").apply { value = action.isAcceptAllCertificates }

    init {
        bodyType.addValueChangeListener { updateBody() }
        updateBody()
        build()
    }

    override fun populate() {
        body.add(url, method, headers, bodyType, formData, json, timeout, ssl)
    }

    override fun writeBack(): Boolean {
        when {
            url.value.isNullOrBlank() -> return fail(url, "Please enter a URL.")
            !isUrl(url.value) -> return fail(url, "The URL is not valid.")
            headers.value.isNotBlank() && !KeyValuePairHelper.isKeyValuePairs(headers.value) ->
                return fail(headers, "Headers must be key=value, one per line.")

            bodyType.value == BodyContentType.FORM_DATA && !KeyValuePairHelper.isKeyValuePairs(formData.value) ->
                return fail(formData, "Form data must be key=value, one per line.")

            bodyType.value == BodyContentType.JSON && !isJson(json.value) ->
                return fail(json, "Not a valid JSON expression.")

            timeout.value == null -> return fail(timeout, "Please enter a timeout.")
            (timeout.value ?: 0) <= 0 -> return fail(timeout, "The timeout must be greater than zero.")
        }
        action.url = url.value
        action.httpRequestMethod = method.value
        action.headers = headers.value
        action.bodyContentType = bodyType.value
        action.formData = formData.value
        action.json = json.value
        action.timeout = timeout.value ?: 0
        action.isAcceptAllCertificates = ssl.value
        return true
    }

    private fun updateBody() {
        formData.isVisible = bodyType.value == BodyContentType.FORM_DATA
        json.isVisible = bodyType.value == BodyContentType.JSON
    }
}

private const val TRIGGER_HELP =
    "Use @TRIGGER@ to insert a description of the VM that caused the trigger to fire."

private val JSON = JsonMapper.builder().build()

private fun artifactNameField(initial: String): TextField =
    TextField("Snapshot name").apply { setWidthFull(); value = initial }

private fun inboxCheckbox(initial: Boolean): Checkbox =
    Checkbox("Send to the inbox of all users with viewing rights").apply { value = initial }

private fun textAreaField(initial: String): TextArea =
    TextArea("Text").apply { setWidthFull(); minHeight = "8rem"; value = initial }

private fun timeRow(time: IntegerField, unit: EnumSelect<TimeUnit>): HorizontalLayout =
    HorizontalLayout(time, unit).apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.END
        isPadding = false
    }

private fun fail(field: HasValidation, message: String): Boolean {
    field.isInvalid = true
    field.errorMessage = message
    return false
}

private fun requireText(field: TextArea): Boolean =
    field.value.isNotBlank() || fail(field, "Please enter some text.")

private fun requireArtifactName(field: TextField): Boolean =
    !field.value.isNullOrBlank() || fail(field, "Please enter a name for the snapshot.")

private fun requireRecordingTime(field: IntegerField): Boolean = when {
    field.value == null -> fail(field, "Please enter a recording time.")
    (field.value ?: 0) <= 0 -> fail(field, "The recording time must be greater than zero.")
    else -> true
}

private fun isUrl(value: String?): Boolean {
    if (value.isNullOrBlank()) {
        return false
    }
    return try {
        URI(value).toURL()
        true
    } catch (_: Exception) {
        false
    }
}

// Blank means "no JSON body"; a bare string literal is rejected (matching V1).
private fun isJson(value: String): Boolean {
    if (value.isBlank()) {
        return true
    }
    return try {
        JSON.readTree(value)
        !value.trim().startsWith("\"")
    } catch (_: Exception) {
        false
    }
}
