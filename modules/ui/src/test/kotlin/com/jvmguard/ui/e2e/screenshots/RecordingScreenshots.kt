package com.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.jvmguard.ui.components.recording.AbstractTransactionDefDialog
import com.jvmguard.ui.components.recording.NamingElementsEditor
import com.jvmguard.ui.components.recording.PolicySubDefDialog
import com.jvmguard.ui.components.recording.telemetries.TelemetryConfigDialog
import com.jvmguard.ui.components.recording.telemetries.TelemetryGrid
import com.jvmguard.ui.components.recording.telemetries.TelemetryLineDialog
import com.jvmguard.ui.components.recording.thresholds.ThresholdDialog
import com.jvmguard.ui.components.recording.thresholds.ThresholdGrid
import com.jvmguard.ui.components.recording.triggers.TriggerActionDialog
import com.jvmguard.ui.components.recording.triggers.TriggerActionsEditor
import com.jvmguard.ui.components.recording.triggers.TriggerDialog
import com.jvmguard.ui.components.recording.triggers.TriggerGrid
import com.jvmguard.ui.views.data.AbstractVmSelectorDialog
import com.jvmguard.ui.views.settings.recording.AbstractRecordingSettingsView
import org.junit.jupiter.api.Test

/** V2 has no Web/EJB transaction type and no per-VM threshold, so some V1 figure names map to the closest equivalent. */
class RecordingScreenshots : ScreenshotTest() {

    @Test
    fun recordingSettings() = onPage {
        login()
        open("recording/transactions")
        assertThat(getByTestId(AbstractRecordingSettingsView.ID_SELECT_BUTTON)).isVisible()
        getByTestId("transaction-grid-matched").waitFor()
        capture("recording_settings")
    }

    @Test
    fun vmGroupOverride() = onPage {
        login()
        open("recording/transactions")
        selectGroup("ERP")
        assertThat(getByTestId(AbstractRecordingSettingsView.ID_OVERRIDE)).isVisible()
        capture("vm_group_override")
    }

    @Test
    fun telemetrySettings() = onPage {
        login()
        open("recording/telemetries")
        getByTestId(TelemetryGrid.ID_GRID).waitFor()
        capture("telemetry_settings")
    }

    @Test
    fun triggers() = onPage {
        login()
        open("recording/triggers")
        getByTestId(TriggerGrid.ID_GRID).waitFor()
        capture("triggers")
    }

    @Test
    fun transactionsConfig() = onPage {
        login()
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        capture("transactions_config")
    }

    @Test
    fun telemetryConfig() = onPage {
        login()
        open("recording/telemetries")
        getByTestId("telemetry-add").click()
        assertThat(getByTestId(TelemetryConfigDialog.ID_SAVE)).isVisible()
        capture("telemetry_config")
    }

    @Test
    fun telemetryLineConfig() = onPage {
        login()
        open("recording/telemetries")
        // A line can only be added after a telemetry is created and confirmed.
        getByTestId("telemetry-add").click()
        getByTestId(TelemetryConfigDialog.ID_NAME).locator("input").fill("Heap usage")
        getByTestId(TelemetryConfigDialog.ID_SAVE).click()
        getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Add line")).last().click()
        assertThat(getByTestId(TelemetryLineDialog.ID_SAVE)).isVisible()
        capture("telemetry_line_config")
    }

    @Test
    fun thresholdConfig() = onPage {
        login()
        open("recording/thresholds")
        getByTestId("threshold-add").click()
        assertThat(getByTestId(ThresholdDialog.ID_SAVE)).isVisible()
        capture("threshold_config")
    }

    @Test
    fun triggersPolicy() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Policy trigger")
        assertThat(getByTestId(TriggerDialog.ID_SAVE)).isVisible()
        capture("triggers_policy") // TODO: unused
    }

    @Test
    fun triggersThreshold() = onPage {
        login()
        // A threshold-violation trigger references a group threshold, so define one first in the same draft.
        open("recording/thresholds")
        getByTestId(ThresholdGrid.ID_GRID).waitFor()
        getByTestId("threshold-add").click()
        getByTestId(ThresholdDialog.ID_TELEMETRY).click()
        val firstType = getByRole(AriaRole.OPTION).first()
        firstType.waitFor()
        firstType.click()
        getByTestId(ThresholdDialog.ID_LOWER_ENABLED).locator("input").check()
        getByTestId(ThresholdDialog.ID_LOWER_VALUE).locator("input").fill("80")
        getByTestId(ThresholdDialog.ID_SAVE).click()
        getByTestId(ThresholdGrid.ID_GRID).waitFor()

        // Navigate within the app (no page reload) so the draft, hence the new threshold, is preserved.
        getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Triggers")).click()
        getByTestId(TriggerGrid.ID_GRID).waitFor()
        addTrigger("Threshold violation trigger")
        assertThat(getByTestId(TriggerDialog.ID_THRESHOLD)).isVisible()
        capture("triggers_threshold") // TODO: unused
    }

    @Test
    fun triggersConnection() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        assertThat(getByTestId(TriggerDialog.ID_SAVE)).isVisible()
        capture("triggers_connection") // TODO: unused
    }

    @Test
    fun triggerActionsList() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        assertThat(getByTestId(TriggerActionsEditor.ID_GRID)).isVisible()
        capture("trigger_actions_list")
    }

    @Test
    fun triggerActions() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        addAction("Create inbox entry")
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("trigger_actions")
    }

    @Test
    fun triggerProfile() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        addAction("Record fine-grained CPU data in profiling mode")
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("trigger_profile")
    }

    @Test
    fun triggerJfrRecording() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        addAction("Record JDK Flight Recorder snapshot")
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("trigger_jfr_recording")
    }

    @Test
    fun triggerHeapDump() = onPage {
        login()
        open("recording/triggers")
        addTrigger("Connection count trigger")
        addAction("Save HPROF memory snapshot")
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("trigger_heap_dump") // TODO: unused
    }

    @Test
    fun matchedType() = onPage {
        login()
        openMatchedDialog()
        capture("matched_type")
    }

    @Test
    fun matchedClass() = onPage {
        login()
        openMatchedDialog()
        // A fresh Matched def already defaults to the "Class or interface" target.
        capture("matched_class")
    }

    @Test
    fun matchedMethod() = onPage {
        login()
        openMatchedDialog()
        selectEnum("Intercept", "Single method of a class or interface")
        capture("matched_method")
    }

    @Test
    fun matchedNaming() = onPage {
        login()
        openMatchedDialog()
        openTab("Naming")
        assertThat(getByTestId(NamingElementsEditor.ID_GRID)).isVisible()
        capture("matched_naming")
    }

    @Test
    fun mappedTransactions() = onPage {
        login()
        openCustomDialog()
        capture("mapped_transactions")
    }

    @Test
    fun mappedNaming() = onPage {
        login()
        openCustomDialog()
        openTab("Naming")
        assertThat(getByTestId(NamingElementsEditor.ID_GRID)).isVisible()
        capture("mapped_naming")
    }

    @Test
    fun instanceNameConfig() = onPage {
        login()
        openMatchedDialog()
        openTab("Naming")
        addNamingElement("Instance name")
        assertThat(getByTestId("naming-element-save")).isVisible()
        capture("instance_name_config")
    }

    @Test
    fun methodParameterConfig() = onPage {
        login()
        openMatchedDialog()
        openTab("Naming")
        addNamingElement("Method parameter")
        assertThat(getByTestId("naming-element-save")).isVisible()
        capture("method_parameter_config")
    }

    @Test
    fun declaredGroup() = onPage {
        login()
        openDeclaredDialog()
        capture("declared_group")
    }

    @Test
    fun declaredFilter() = onPage {
        login()
        openDeclaredDialog()
        openTab("Filter")
        capture("declared_filter")
    }

    @Test
    fun policySubdef() = onPage {
        login()
        openPolicySubdefDialog()
        assertThat(getByTestId(PolicySubDefDialog.ID_SAVE)).isVisible()
        capture("policy_subdef")
    }

    @Test
    fun policySubdefFilter() = onPage {
        login()
        openPolicySubdefDialog()
        // The sub-def dialog already opens on its "Filter" tab.
        assertThat(getByTestId(PolicySubDefDialog.ID_SAVE)).isVisible()
        capture("policy_subdef_filter")
    }

    @Test
    fun transactionsSet() = onPage {
        login()
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        getByTestId("set-add").click()
        assertThat(locator("vaadin-dialog-overlay")).isVisible()
        capture("transactions_set")
    }

    private fun Page.selectGroup(name: String) {
        getByTestId(AbstractRecordingSettingsView.ID_SELECT_BUTTON).click()
        getByText(name, Page.GetByTextOptions().setExact(true)).first().click()
        getByTestId(AbstractVmSelectorDialog.ID_SELECT).click()
    }

    private fun Page.addTrigger(typeLabel: String) {
        getByTestId("trigger-add").click()
        getByText(typeLabel).click()
        getByTestId(TriggerDialog.ID_SAVE).waitFor()
    }

    private fun Page.addAction(actionLabel: String) {
        getByTestId(TriggerActionsEditor.ID_ADD).click()
        getByText(actionLabel).click()
        getByTestId(TriggerActionDialog.ID_SAVE).waitFor()
    }

    private fun Page.openMatchedDialog() {
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText("Matched")).first().click()
        getByTestId("transaction-grid-matched").waitFor()
        getByTestId("transaction-add").click()
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).waitFor()
    }

    private fun Page.openDeclaredDialog() {
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText("Declared")).first().click()
        getByTestId("transaction-grid-declared").waitFor()
        getByTestId("transaction-add").click()
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).waitFor()
    }

    private fun Page.openCustomDialog() {
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText("Mapped")).first().click()
        getByTestId("transaction-grid-mapped").waitFor()
        getByTestId("transaction-add").click()
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).waitFor()
    }

    private fun Page.openTab(label: String) {
        // No overlay scoping: the tabs render outside the dialog overlay, so scoping to it matches nothing.
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText(label)).first().click()
    }

    private fun Page.selectEnum(label: String, option: String) {
        locator("vaadin-select")
            .filter(Locator.FilterOptions().setHasText(label)).first().click()
        getByText(option, Page.GetByTextOptions().setExact(true)).click()
    }

    private fun Page.addNamingElement(label: String) {
        // The naming editor is enabled only once a custom transaction name is requested.
        getByTestId("naming-active").locator("input").check()
        getByTestId(NamingElementsEditor.ID_ADD).click()
        getByText(label, Page.GetByTextOptions().setExact(true)).click()
        getByTestId("naming-element-save").waitFor()
    }

    private fun Page.openPolicySubdefDialog() {
        open("recording/transactions")
        getByTestId("transaction-grid-matched").waitFor()
        // The row menu offers "Add policy specialization" only on a saved Matched def.
        getByTestId("transaction-add").click()
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).waitFor()
        // A class filter is required to save the definition.
        locator("vaadin-text-field")
            .filter(Locator.FilterOptions().setHasText("Class or interface name"))
            .first().locator("input").fill("com.example.Service")
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).click()
        locator("[data-testid^='transaction-row-menu-']").first().click()
        getByText("Add policy specialization").click()
        getByTestId(PolicySubDefDialog.ID_SAVE).waitFor()
    }
}
