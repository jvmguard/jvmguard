package com.jvmguard.ui.views.account

import com.jvmguard.common.helper.ApiKeyGenerator
import com.jvmguard.data.user.User
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.AbstractAccountSectionView
import com.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "account/api-key", layout = MainLayout::class)
@PageTitle("jvmguard: Account")
class AccountApiKeyView : AbstractAccountSectionView() {

    private val apiKeyField = TextField("API key").apply {
        isReadOnly = true
        setWidthFull()
        testId = ID_API_KEY
    }

    private val keyRow = HorizontalLayout(apiKeyField, copyButton()).apply {
        setWidthFull()
        setFlexGrow(1.0, apiKeyField)
        defaultVerticalComponentAlignment = FlexComponent.Alignment.END
        isVisible = false
    }

    init {
        val intro = Span(
            "When using the jvmguard REST API, you authenticate with your user name and an API key. The REST " +
                    "API must be enabled by setting restApiEnabled to true in config/application.yaml.",
        )
        val generate = Button("Generate new API key", VaadinIcon.KEY.create()) { generate() }.apply {
            testId = ID_GENERATE
        }
        val warning = Span("The new key replaces the old one when you save, and is shown only once. Copy it before saving.")
            .apply { addClassName("jvmguard-field-hint") }
        add(settingsSection("REST API key", intro, generate, keyRow, warning))
    }

    override fun bind(binder: Binder<User>) {}

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        Sessions.accountDraft().pendingApiKey?.let { showKey(it) }
    }

    private fun generate() {
        val key = ApiKeyGenerator.generate()
        Sessions.accountDraft().apply {
            pendingApiKey = key
            markDirty()
        }
        showKey(key)
    }

    private fun showKey(key: String) {
        apiKeyField.value = key
        keyRow.isVisible = true
    }

    private fun copyButton(): Button = Button(VaadinIcon.COPY.create()) {
        apiKeyField.element.executeJs("navigator.clipboard && navigator.clipboard.writeText(this.value)")
    }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        setAriaLabel("Copy API key")
        setTooltipText("Copy")
    }

    companion object {
        const val ID_GENERATE = "account-generate-key"
        const val ID_API_KEY = "account-api-key"
    }
}
