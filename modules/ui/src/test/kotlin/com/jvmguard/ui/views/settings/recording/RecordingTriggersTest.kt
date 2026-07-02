package com.jvmguard.ui.views.settings.recording

import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.ConnectionTrigger
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.config.triggers.TriggerType
import com.jvmguard.data.config.triggers.actions.EmailAction
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.recording.sets.SaveSetDialog
import com.jvmguard.ui.components.recording.triggers.TriggerActionDialog
import com.jvmguard.ui.components.recording.triggers.TriggerDialog
import com.jvmguard.ui.components.recording.triggers.TriggerGrid
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordingTriggersTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: ServerConnection

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearRecordingDraft()
        Sessions.resetRecordingSelection()
    }

    private fun rootGroup() = Sessions.recordingDraft().groupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)!!
    private fun rootTriggers() = rootGroup().triggerSettings.triggers

    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    private fun cpuType(): TelemetryType = connection.idToTelemetryType.values.first { it.name == "CPU load" }

    @Test
    fun rootShowsTriggerGridAndAddDropdown() {
        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        find<TriggerGrid>().single()
        assertTrue(find<MenuBar>().all().any { it.testId == "trigger-add" })
    }

    @Test
    fun addingAConnectionTriggerPersists() {
        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        find<TriggerGrid>().single().addTrigger(TriggerType.CONNECTION)
        use(find<Button>().from(find<TriggerDialog>().single()).all().first { it.text == "Save" }).click()

        assertEquals(1, rootTriggers().size)
        assertTrue(rootTriggers().single() is ConnectionTrigger)

        use(shellSave()).click()
        assertTrue(connection.groupConfigs.first { it.isRoot }.triggerSettings.triggers.any { it is ConnectionTrigger })
    }

    @Test
    fun addedTriggerGetsAnId() {
        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        find<TriggerGrid>().single().addTrigger(TriggerType.CONNECTION)
        use(find<Button>().from(find<TriggerDialog>().single()).all().first { it.text == "Save" }).click()

        // The collector correlates trigger runtime state by id, so a new trigger must get one.
        assertNotNull(rootTriggers().single().id)
    }

    @Test
    fun thresholdTriggerReferencesAGroupThreshold() {
        rootGroup().thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = cpuType().telemetryIdentifier
            isUpperBoundEnabled = true
            upperBound = 90
        })

        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        find<TriggerGrid>().single().addTrigger(TriggerType.THRESHOLD)

        val dialog = find<TriggerDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val select = find<Select<*>>().from(dialog).all().first { it.testId == TriggerDialog.ID_THRESHOLD } as Select<Threshold>
        select.value = rootGroup().thresholdSettings.thresholds.first()
        use(find<Button>().from(dialog).all().first { it.text == "Save" }).click()

        val trigger = rootTriggers().single() as ThresholdTrigger
        assertEquals(cpuType().telemetryIdentifier, trigger.thresholdIdentifier?.telemetryIdentifier)
    }

    @Test
    fun actionDialogWritesFields() {
        val action = EmailAction()
        var saved = false
        val dialog = TriggerActionDialog.create(action, isNew = true) { saved = true }
        dialog.open()
        use(find<TextField>().from(dialog).all().first { it.label == "Email" }).setValue("ops@example.com")
        use(find<TextArea>().from(dialog).all().first { it.label == "Text" }).setValue("Heap is high")
        use(find<Button>().from(dialog).all().first { it.text == "Save" }).click()

        assertTrue(saved)
        assertEquals("ops@example.com", action.email)
    }

    @Test
    fun savingATriggerSetStoresItOnTheServer() {
        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        find<TriggerGrid>().single().addTrigger(TriggerType.CONNECTION)
        use(find<Button>().from(find<TriggerDialog>().single()).all().first { it.text == "Save" }).click()

        use(find<Button>().all().first { it.text == "Save set" }).click()
        val setDialog = find<SaveSetDialog<*, *>>().single()
        use(find<TextField>().from(setDialog).all().first()).setValue("Standard triggers")
        use(find<Button>().from(setDialog).all().first { it.text == "Save" }).click()

        assertTrue(connection.triggerSets.any { it.name == "Standard triggers" && it.items.isNotEmpty() })
    }
}
