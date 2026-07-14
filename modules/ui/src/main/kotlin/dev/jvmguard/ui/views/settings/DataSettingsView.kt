package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.transactions.CapType
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.badge.Badge
import com.vaadin.flow.component.badge.BadgeVariant
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.slider.IntegerSlider
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/data", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class DataSettingsView : AbstractSettingsSectionView() {

    private val transactions = daySlider("Keep transactions for", 2, 365)
    private val indefinitely = Checkbox("Keep transactions indefinitely").apply {
        testId = ID_INDEFINITELY
        addValueChangeListener { transactions.isEnabled = !value }
    }
    private val violations = daySlider("Keep threshold violations for", 1, 200).apply {
        addClassName("jvmguard-settings-gap-before")
    }

    private val snapshotDays = IntegerField("Delete snapshots older than (days, 0 = keep forever)").apply {
        testId = ID_SNAPSHOT_DAYS
        addClassName("jvmguard-nowrap-label")
        addClassName("jvmguard-settings-gap-before")
        min = 0
        width = "10rem"
    }

    private val transactionCap = IntegerField("Maximum distinct transaction names").apply {
        testId = ID_TRANSACTION_CAP
        addClassName("jvmguard-nowrap-label")
        min = 1
        width = "10rem"
    }
    private val capStatus = Badge().apply { testId = ID_CAP_STATUS }
    private val resetCap = Button("Reset cap counter") { resetCaps() }

    init {
        val capRow = HorizontalLayout(transactionCap, capStatus).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
            style.set("gap", "0.75rem")
        }
        add(settingsSection("Data retention", transactions, indefinitely, violations, snapshotDays, capRow, resetCap))
        refreshCapStatus()
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(transactions)
            .withConverter({ it.toInt() }, { it.coerceIn(2, 365) })
            .bind({ it.fixedTransactionDays }, { config, value -> config.fixedTransactionDays = value })
        binder.forField(indefinitely)
            .bind({ it.infiniteTransactionDays }, { config, value -> config.infiniteTransactionDays = value })
        binder.forField(violations)
            .withConverter({ it.toInt() }, { it.coerceIn(1, 200) })
            .bind({ it.violationDays }, { config, value -> config.violationDays = value })
        binder.forField(snapshotDays)
            .asRequired("Enter a number of days.")
            .bind({ it.snapshotFileDays }, { config, value -> config.snapshotFileDays = value })
        binder.forField(transactionCap)
            .asRequired("Enter a maximum.")
            .bind({ it.transactionCap }, { config, value -> config.transactionCap = value })
    }

    private fun refreshCapStatus() {
        val reached = Sessions.current()?.serverConnection?.caps?.contains(CapType.TRANSACTION) == true
        capStatus.removeThemeVariants(BadgeVariant.WARNING)
        if (reached) {
            capStatus.text = "The cap has been reached."
            capStatus.icon = VaadinIcon.WARNING.create()
            capStatus.addThemeVariants(BadgeVariant.WARNING)
        } else {
            capStatus.text = "The cap has not been reached."
            capStatus.icon = null
        }
    }

    private fun resetCaps() {
        Sessions.current()?.serverConnection?.resetCaps()
        refreshCapStatus()
        Notifications.show("Cap counters reset.")
    }

    companion object {
        const val ID_INDEFINITELY = "settings-tx-indefinitely"
        const val ID_SNAPSHOT_DAYS = "settings-snapshot-days"
        const val ID_TRANSACTION_CAP = "settings-transaction-cap"
        const val ID_CAP_STATUS = "settings-cap-status"

        private fun daySlider(label: String, min: Int, max: Int): IntegerSlider =
            IntegerSlider(label, min, max).apply {
                setStep(1)
                isMinMaxVisible = true
                setWidthFull()
                addValueChangeListener { setLabel("$label: ${(it.value ?: min.toDouble()).toInt()} days") }
            }
    }
}
