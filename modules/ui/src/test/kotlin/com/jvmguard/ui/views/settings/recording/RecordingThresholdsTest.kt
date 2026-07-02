package com.jvmguard.ui.views.settings.recording

import com.jvmguard.common.helper.ListModification
import com.jvmguard.data.config.sets.ThresholdSet
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.recording.sets.AddSetDialog
import com.jvmguard.ui.components.recording.sets.SaveSetDialog
import com.jvmguard.ui.components.recording.thresholds.ThresholdDialog
import com.jvmguard.ui.components.recording.thresholds.ThresholdGrid
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordingThresholdsTest : JvmGuardBrowserlessTest() {

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

    private fun rootThresholds() =
        Sessions.recordingDraft().groupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)!!.thresholdSettings.thresholds

    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    private fun testId(id: String): (Component) -> Boolean = { it.testId == id }

    private fun cpuType(): TelemetryType = connection.idToTelemetryType.values.first { it.name == "CPU load" }

    private fun addThreshold(lowerBound: Int) {
        find<ThresholdGrid>().single().addNew()
        val dialog = find<ThresholdDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val telemetry = find<Select<*>>(dialog).all().first(testId(ThresholdDialog.ID_TELEMETRY)) as Select<TelemetryType>
        telemetry.value = cpuType()
        find<Checkbox>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_ENABLED)).value = true
        use(find<IntegerField>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_VALUE))).setValue(lowerBound)
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()
    }

    @Test
    fun rootShowsThresholdGrid() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        find<ThresholdGrid>().single()
    }

    @Test
    fun addingAThresholdPersistsToTheGroup() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        addThreshold(lowerBound = 80)

        assertEquals(1, rootThresholds().size)
        assertTrue(rootThresholds().single().isLowerBoundEnabled)

        use(shellSave()).click()
        val saved = connection.groupConfigs.first { it.isRoot }.thresholdSettings.thresholds
        assertTrue(saved.any { it.isLowerBoundEnabled && it.lowerBound == 80L })
    }

    @Test
    fun aThresholdWithoutAnyBoundIsRejected() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        find<ThresholdGrid>().single().addNew()
        val dialog = find<ThresholdDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val telemetry = find<Select<*>>(dialog).all().first(testId(ThresholdDialog.ID_TELEMETRY)) as Select<TelemetryType>
        telemetry.value = cpuType()
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(rootThresholds().isEmpty())
    }

    @Test
    fun aNonPositiveBoundIsRejected() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        find<ThresholdGrid>().single().addNew()
        val dialog = find<ThresholdDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val telemetry = find<Select<*>>(dialog).all().first(testId(ThresholdDialog.ID_TELEMETRY)) as Select<TelemetryType>
        telemetry.value = cpuType()
        find<Checkbox>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_ENABLED)).value = true
        use(find<IntegerField>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_VALUE))).setValue(0)
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(rootThresholds().isEmpty())
    }

    @Test
    fun aDuplicateThresholdOnTheSameTelemetryIsRejected() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        addThreshold(lowerBound = 80)
        assertEquals(1, rootThresholds().size)

        // Same telemetry and no custom name yields the same identifier, so the duplicate is rejected.
        find<ThresholdGrid>().single().addNew()
        val dialog = find<ThresholdDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val telemetry = find<Select<*>>(dialog).all().first(testId(ThresholdDialog.ID_TELEMETRY)) as Select<TelemetryType>
        telemetry.value = cpuType()
        find<Checkbox>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_ENABLED)).value = true
        use(find<IntegerField>(dialog).all().first(testId(ThresholdDialog.ID_LOWER_VALUE))).setValue(50)
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertEquals(1, rootThresholds().size)
    }

    @Test
    fun savingASetStoresItOnTheServer() {
        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        addThreshold(lowerBound = 50)

        use(find<Button>().all().first { it.text == "Save set" }).click()
        val dialog = find<SaveSetDialog<*, *>>().single()
        use(find<TextField>(dialog).all().first()).setValue("CPU thresholds")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(connection.thresholdSets.any { it.name == "CPU thresholds" && it.items.isNotEmpty() })
    }

    @Test
    fun addingASetAppendsItsThresholds() {
        val set = ThresholdSet("Imported", listOf(Threshold().apply {
            telemetryIdentifier = cpuType().telemetryIdentifier
            isUpperBoundEnabled = true
            upperBound = 90
        }))
        connection.applyListModification(
            ListModification(emptyList(), emptyList(), listOf(set), ThresholdSet::class.java),
        )

        UI.getCurrent().navigate(RecordingThresholdsView::class.java)
        use(find<Button>().all().first { it.text == "Add set" }).click()
        val dialog = find<AddSetDialog<*, *>>().single()

        @Suppress("UNCHECKED_CAST")
        val grid = find<Grid<*>>(dialog).single() as Grid<ThresholdSet>
        grid.select(grid.genericDataView.items.toList().first { it.name == "Imported" })
        use(find<Button>(dialog).all().first { it.text == "Add" }).click()

        assertTrue(rootThresholds().any { it.isUpperBoundEnabled && it.upperBound == 90L })
    }
}
