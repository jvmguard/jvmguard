package dev.jvmguard.ui.views.vms

import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.data.user.viewsettings.SparkLineMemento
import dev.jvmguard.data.user.viewsettings.SparkLineScaleMode
import dev.jvmguard.data.user.viewsettings.VmPanelSettings
import dev.jvmguard.data.vmdata.SparkLineRange
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmFilter
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.server.ModificationListener
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.ui.server.registerModificationListener
import dev.jvmguard.ui.shell.CachedView
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "", layout = MainLayout::class)
@PageTitle("jvmguard: JVMs")
class VmsView : VerticalLayout(), ModificationListener, CachedView {

    private val filterSelect = EnumSelect("Shown JVMs", VmFilter::class.java) { filterLabel(it) }.apply {
        testId = ID_FILTER_SELECT
        value = VmFilter.CONNECTED
        addUserValueChangeListener(::controlsChanged)
    }
    private val rangeSelect = EnumSelect("Range", SparkLineRange::class.java) { it.toString() }.apply {
        testId = ID_RANGE_SELECT
        value = SparkLineRange.LAST_HOUR
        addUserValueChangeListener(::controlsChanged)
    }
    private val scaleModeSelect = EnumSelect("Scale", SparkLineScaleMode::class.java) { scaleModeLabel(it) }.apply {
        testId = ID_SCALE_SELECT
        value = SparkLineScaleMode.SEPARATE
        addUserValueChangeListener(::controlsChanged)
    }
    private val columnChooser = MultiSelectComboBox<TelemetryType>("Telemetries").apply {
        testId = ID_TELEMETRIES
        setItemLabelGenerator { it.name }
        minWidth = TELEMETRIES_MIN_WIDTH
        maxWidth = TELEMETRIES_MAX_WIDTH
        setAutoExpand(MultiSelectComboBox.AutoExpandMode.HORIZONTAL)
        addValueChangeListener { if (it.isFromClient) columnsChanged() }
    }
    private val grid = VmTreeGrid()

    private var visibleTelemetryTypes: List<TelemetryType> = emptyList()
    private var initialized = false

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false

        val toolbar = HorizontalLayout(filterSelect, rangeSelect, scaleModeSelect, columnChooser).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = true
            setWidthFull()
            isWrap = true
        }
        add(toolbar, grid)
        setFlexGrow(1.0, grid)
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        val session = Sessions.current() ?: return
        registerModificationListener(session)
        addDetachListener { it.unregisterListener() }
        if (initialized) {
            reloadGrid()
        } else {
            initialized = true
            initialize(session)
        }
    }

    override fun modifyNotified(modificationTypes: Set<ModificationType>) {
        reloadGrid()
    }

    private fun initialize(session: UserSession) {
        val settings = session.viewSettings.vmPanelSettings
        filterSelect.value = settings.vmFilter
        rangeSelect.value = settings.sparkLineRange
        scaleModeSelect.value = settings.sparkLineScaleMode
        val available = sortedTypes(session.serverConnection.idToTelemetryType.values)
        columnChooser.setItems(available)
        visibleTelemetryTypes = visibleFromSettings(settings, available)
        columnChooser.value = LinkedHashSet(visibleTelemetryTypes)
        grid.setVisibleTelemetryTypes(visibleTelemetryTypes)
        reloadGrid()
    }

    private fun controlsChanged() {
        persistSettings()
        reloadGrid()
    }

    private fun columnsChanged() {
        visibleTelemetryTypes = sortedTypes(columnChooser.value)
        grid.setVisibleTelemetryTypes(visibleTelemetryTypes)
        persistSettings()
        reloadGrid()
    }

    private fun reloadGrid() {
        grid.reload(filterSelect.value, rangeSelect.value, scaleModeSelect.value)
    }

    private fun persistSettings() {
        val session = Sessions.current() ?: return
        val settings = session.viewSettings.vmPanelSettings
        settings.vmFilter = filterSelect.value
        settings.sparkLineRange = rangeSelect.value
        settings.sparkLineScaleMode = scaleModeSelect.value
        settings.sparkLineMementos = visibleTelemetryTypes.mapTo(ArrayList()) { SparkLineMemento(it) }
        session.saveViewSettings()
    }

    companion object {
        const val ID_FILTER_SELECT = "vms-filter"
        const val ID_RANGE_SELECT = "vms-range"
        const val ID_SCALE_SELECT = "vms-scale"
        const val ID_TELEMETRIES = "vms-telemetries"

        private const val TELEMETRIES_MIN_WIDTH = "16rem"
        private const val TELEMETRIES_MAX_WIDTH = "48rem"

        private fun scaleModeLabel(scaleMode: SparkLineScaleMode): String = when (scaleMode) {
            SparkLineScaleMode.SEPARATE -> "Separate"
            SparkLineScaleMode.GROUP -> "Common per group"
            SparkLineScaleMode.COMMON -> "Common"
        }

        private fun filterLabel(filter: VmFilter): String = filter.toString().replace(" JVMs", "")

        private fun sortedTypes(types: Collection<TelemetryType>): List<TelemetryType> =
            types.sortedBy { it.name }

        private fun visibleFromSettings(
            settings: VmPanelSettings, available: List<TelemetryType>
        ): List<TelemetryType> {
            val visible = mutableListOf<TelemetryType>()
            for (memento in settings.sparkLineMementos) {
                available.filterTo(visible) { memento.matches(it) }
            }
            return sortedTypes(visible)
        }
    }
}
