package dev.jvmguard.ui.views.account

import dev.jvmguard.common.helper.ApiKeyGenerator
import dev.jvmguard.data.user.User
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.server.ServerUrls
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.settings.AbstractAccountSectionView
import dev.jvmguard.ui.views.settings.settingsSection
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

    private val harnessSelect = EnumSelect("Coding assistant", McpHarness::class.java) { it.label }.apply {
        value = McpHarness.CLAUDE_CODE
        testId = ID_MCP_HARNESS
        addUserValueChangeListener { refreshMcpSnippet() }
    }

    private val mcpInstruction = Span().apply { addClassName("jvmguard-field-hint") }

    private val mcpSnippet = Span().apply {
        addClassName("jvmguard-mcp-snippet")
        testId = ID_MCP_SNIPPET
    }

    private val mcpSnippetBlock = HorizontalLayout(mcpSnippet, mcpCopyButton()).apply {
        addClassName("jvmguard-code-block")
        setWidthFull()
        defaultVerticalComponentAlignment = FlexComponent.Alignment.START
        expand(mcpSnippet)
        isPadding = false
    }

    init {
        val intro = Span(
            "When using the jvmguard REST API or MCP server, you authenticate with an API key that you " +
                    "generate here.",
        )
        val generate = Button("Generate new API key", VaadinIcon.KEY.create()) { generate() }.apply {
            testId = ID_GENERATE
        }
        val warning = Span("The new key replaces the old one when you save, and is shown only once. Copy it before saving.")
            .apply { addClassName("jvmguard-field-hint") }
        add(settingsSection("API key", intro, generate, keyRow, warning))

        val mcpIntro = Span(
            "Connect an AI coding assistant to this server's MCP endpoint. The assistant authenticates with " +
                    "your API key as a bearer token.",
        )
        val mcpKeyHint = Span(
            "Your API key is only shown right after you generate it. Generate a key above, then copy the " +
                    "snippet below while the key is still visible. Otherwise it shows a placeholder.",
        ).apply { addClassName("jvmguard-field-hint") }
        add(settingsSection("MCP server integration", mcpIntro, harnessSelect, mcpInstruction, mcpSnippetBlock, mcpKeyHint))
    }

    override fun bind(binder: Binder<User>) {}

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        Sessions.accountDraft().pendingApiKey?.let { showKey(it) }
        refreshMcpSnippet()
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
        refreshMcpSnippet()
    }

    private fun refreshMcpSnippet() {
        val harness = harnessSelect.value ?: McpHarness.CLAUDE_CODE
        val key = Sessions.peekAccountDraft()?.pendingApiKey ?: PLACEHOLDER_KEY
        mcpInstruction.text = harness.instruction
        mcpSnippet.text = harness.snippet("${ServerUrls.baseUrl()}/mcp", key)
    }

    private fun copyButton(): Button = Button(VaadinIcon.COPY.create()) {
        apiKeyField.element.executeJs("navigator.clipboard && navigator.clipboard.writeText(this.value)")
    }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        setAriaLabel("Copy API key")
        setTooltipText("Copy")
    }

    private fun mcpCopyButton(): Button = Button(VaadinIcon.COPY_O.create()) {
        mcpSnippet.element.executeJs($$"if (navigator.clipboard) { navigator.clipboard.writeText($0); }", mcpSnippet.text)
        Notifications.show("Copied to clipboard.")
    }.apply {
        addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL)
        setAriaLabel("Copy MCP setup snippet")
        setTooltipText("Copy")
    }

    @Suppress("unused")
    private enum class McpHarness(val label: String, val instruction: String) {
        CLAUDE_CODE("Claude Code", "Run this command:") {
            override fun snippet(url: String, key: String) =
                @Suppress("SpellCheckingInspection")
                "claude mcp add --transport http jvmguard $url --header \"Authorization: Bearer $key\""
        },
        CODEX("Codex", "Add this to ~/.codex/config.toml:") {
            override fun snippet(url: String, key: String) = """
                [mcp_servers.jvmguard]
                url = "$url"
                http_headers = { "Authorization" = "Bearer $key" }
            """.trimIndent()
        },
        ANTIGRAVITY("Antigravity", "Add this to ~/.gemini/antigravity/mcp_config.json:") {
            override fun snippet(url: String, key: String) = """
                {
                  "mcpServers": {
                    "jvmguard": {
                      "serverUrl": "$url",
                      "headers": { "Authorization": "Bearer $key" }
                    }
                  }
                }
            """.trimIndent()
        },
        OPEN_CODE("OpenCode", "Add this to opencode.json (project) or ~/.config/opencode/opencode.json:") {
            override fun snippet(url: String, key: String) = $$"""
                {
                  "$schema": "https://opencode.ai/config.json",
                  "mcp": {
                    "jvmguard": {
                      "type": "remote",
                      "url": "$$url",
                      "enabled": true,
                      "headers": { "Authorization": "Bearer $$key" }
                    }
                  }
                }
            """.trimIndent()
        },
        CURSOR("Cursor", "Add this to ~/.cursor/mcp.json (global) or .cursor/mcp.json (project):") {
            override fun snippet(url: String, key: String) = """
                {
                  "mcpServers": {
                    "jvmguard": {
                      "url": "$url",
                      "headers": { "Authorization": "Bearer $key" }
                    }
                  }
                }
            """.trimIndent()
        },
        ;

        abstract fun snippet(url: String, key: String): String
    }

    companion object {
        const val ID_GENERATE = "account-generate-key"
        const val ID_API_KEY = "account-api-key"
        const val ID_MCP_HARNESS = "account-mcp-harness"
        const val ID_MCP_SNIPPET = "account-mcp-snippet"
        private const val PLACEHOLDER_KEY = "<YOUR_API_KEY>"
    }
}
