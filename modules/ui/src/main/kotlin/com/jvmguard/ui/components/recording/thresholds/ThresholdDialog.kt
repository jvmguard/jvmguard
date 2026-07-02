package com.jvmguard.ui.components.recording.thresholds

import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.ThresholdIdentifier
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.Notifications
import com.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class ThresholdDialog(
    private val threshold: Threshold,
    isNew: Boolean,
    private val telemetryTypes: Collection<TelemetryType>,
    private val siblings: List<Threshold>,
    private val onSave: (Threshold) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(Threshold::class.java)

    private val telemetrySelect = Select<TelemetryType>().apply {
        label = "Telemetry"
        testId = ID_TELEMETRY
        setItemLabelGenerator { it.name }
        setItems(telemetryTypes.sortedWith(compareBy({ it.categoryName }, { it.name })))
        addValueChangeListener { updateUnits() }
    }
    private val customName = TextField("Custom name (optional)").apply { setWidthFull() }
    private val target = EnumSelect("Threshold target", Threshold.Target::class.java) { it.toString() }

    private val lowerEnabled = Checkbox("Lower bound").apply {
        testId = ID_LOWER_ENABLED
        addValueChangeListener { updateEnabled() }
    }
    private val lowerValue = IntegerField().apply { width = "10rem"; testId = ID_LOWER_VALUE }
    private val lowerUnit = Select<Int>().apply { width = "8rem" }

    private val upperEnabled = Checkbox("Upper bound").apply {
        testId = ID_UPPER_ENABLED
        addValueChangeListener { updateEnabled() }
    }
    private val upperValue = IntegerField().apply { width = "10rem"; testId = ID_UPPER_VALUE }
    private val upperUnit = Select<Int>().apply { width = "8rem" }

    private val minimumTime = IntegerField("Minimum time").apply { width = "8rem" }
    private val minimumTimeUnit = EnumSelect("", TimeUnit::class.java) { it.toString() }
    private val inhibitTime = IntegerField("Inhibit duplicates for").apply { width = "8rem" }
    private val inhibitTimeUnit = EnumSelect("", TimeUnit::class.java) { it.toString() }
    private val continuous = Checkbox("No duplicates while the threshold remains continuously violated")

    init {
        headerTitle = if (isNew) "Add threshold" else "Edit threshold"
        width = "44rem"
        isResizable = false

        add(
            VerticalLayout(
                telemetrySelect,
                customName,
                target,
                boundRow(lowerEnabled, lowerValue, lowerUnit).apply { addClassName("jvmguard-settings-gap-before") },
                boundRow(upperEnabled, upperValue, upperUnit).apply { addClassName("jvmguard-settings-gap-after") },
                timeRow(minimumTime, minimumTimeUnit),
                timeRow(inhibitTime, inhibitTimeUnit),
                continuous,
            ).apply { isPadding = false; isSpacing = true })

        bind()
        read()

        confirmFooter("Save", ID_SAVE) { save() }
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(target).bind({ it.target }, { t, v -> t.target = v })
        binder.forField(lowerEnabled).bind({ it.isLowerBoundEnabled }, { t, v -> t.isLowerBoundEnabled = v })
        binder.forField(upperEnabled).bind({ it.isUpperBoundEnabled }, { t, v -> t.isUpperBoundEnabled = v })
        binder.forField(minimumTime).bind({ it.minimumTime }, { t, v -> t.minimumTime = v ?: 0 })
        binder.forField(minimumTimeUnit).bind({ it.minimumTimeUnit }, { t, v -> t.minimumTimeUnit = v })
        binder.forField(inhibitTime).bind({ it.inhibitDuplicateTime }, { t, v -> t.inhibitDuplicateTime = v ?: 0 })
        binder.forField(inhibitTimeUnit).bind({ it.inhibitDuplicateTimeUnit }, { t, v -> t.inhibitDuplicateTimeUnit = v })
        binder.forField(continuous)
            .bind({ it.isInhibitDuplicateForContinuousViolation }, { t, v -> t.isInhibitDuplicateForContinuousViolation = v })
    }

    private fun read() {
        telemetrySelect.value = telemetryTypeOf(threshold.telemetryIdentifier, telemetryTypes)
        updateUnits()
        customName.value = threshold.customName.value
        lowerValue.value = threshold.lowerBound.toInt()
        upperValue.value = threshold.upperBound.toInt()
        binder.readBean(threshold)
        lowerUnit.value = threshold.lowerBoundUnitLevel.takeIf { lowerUnit.listDataView.itemCount > it }
        upperUnit.value = threshold.upperBoundUnitLevel.takeIf { upperUnit.listDataView.itemCount > it }
        updateEnabled()
    }

    private fun save() {
        val type = telemetrySelect.value
        if (type == null) {
            telemetrySelect.isInvalid = true
            telemetrySelect.errorMessage = "Select a telemetry."
            return
        }
        if (!lowerEnabled.value && !upperEnabled.value) {
            Notifications.show("Please enable either a lower or an upper bound.")
            return
        }
        if (lowerEnabled.value && (lowerValue.value ?: 0) <= 0 || upperEnabled.value && (upperValue.value ?: 0) <= 0) {
            Notifications.show("Enter a positive value for each enabled bound.")
            return
        }
        val candidate = ThresholdIdentifier(type.telemetryIdentifier, customName.value.trim())
        if (siblings.any { ThresholdIdentifier(it.telemetryIdentifier, it.customName.usedValue) == candidate }) {
            customName.isInvalid = true
            customName.errorMessage = "A threshold for this telemetry already exists. Assign a distinct custom name."
            return
        }
        if (!binder.writeBeanIfValid(threshold)) {
            return
        }
        threshold.telemetryIdentifier = type.telemetryIdentifier
        threshold.customName.value = customName.value
        threshold.customName.isChecked = customName.value.isNotBlank()
        threshold.lowerBound = (lowerValue.value ?: 0).toLong()
        threshold.lowerBoundUnitLevel = lowerUnit.value ?: 0
        threshold.upperBound = (upperValue.value ?: 0).toLong()
        threshold.upperBoundUnitLevel = upperUnit.value ?: 0
        onSave(threshold)
        close()
    }

    private fun updateUnits() {
        val labels = telemetrySelect.value?.unit?.labels ?: emptyArray()
        listOf(lowerUnit, upperUnit).forEach { select ->
            val previous = select.value
            select.setItems((labels.indices).toList())
            select.setItemLabelGenerator { labels.getOrElse(it) { "" } }
            select.isVisible = labels.size > 1
            select.value = previous?.takeIf { it < labels.size } ?: 0.takeIf { labels.isNotEmpty() }
        }
    }

    private fun updateEnabled() {
        lowerValue.isEnabled = lowerEnabled.value
        lowerUnit.isEnabled = lowerEnabled.value
        upperValue.isEnabled = upperEnabled.value
        upperUnit.isEnabled = upperEnabled.value
    }

    private fun boundRow(enabled: Checkbox, value: IntegerField, unit: Select<Int>): HorizontalLayout =
        HorizontalLayout(enabled, value, unit).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
        }

    private fun timeRow(value: IntegerField, unit: EnumSelect<TimeUnit>): HorizontalLayout =
        HorizontalLayout(value, unit).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
        }

    companion object {
        const val ID_TELEMETRY = "threshold-telemetry"
        const val ID_LOWER_ENABLED = "threshold-lower-enabled"
        const val ID_LOWER_VALUE = "threshold-lower-value"
        const val ID_UPPER_ENABLED = "threshold-upper-enabled"
        const val ID_UPPER_VALUE = "threshold-upper-value"
        const val ID_SAVE = "threshold-save"
    }
}
