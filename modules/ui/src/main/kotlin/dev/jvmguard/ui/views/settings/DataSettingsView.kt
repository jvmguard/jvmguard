package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.transactions.CapType
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
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
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/data", layout = MainLayout::class)
class DataSettingsView : AbstractSettingsSectionView() {

    private val transactions = daySlider(t("settings.data.keepTransactions"), 2, 365)
    private val indefinitely = Checkbox(t("settings.data.keepIndefinitely")).apply {
        testId = ID_INDEFINITELY
        addValueChangeListener { transactions.isEnabled = !value }
    }
    private val violations = daySlider(t("settings.data.keepViolations"), 1, 200).apply {
        addClassName("jvmguard-settings-gap-before")
    }

    private val snapshotDays = IntegerField(t("settings.data.snapshotDays")).apply {
        testId = ID_SNAPSHOT_DAYS
        addClassName("jvmguard-nowrap-label")
        addClassName("jvmguard-settings-gap-before")
        min = 0
        width = "10rem"
    }

    private val transactionCap = IntegerField(t("settings.data.transactionCap")).apply {
        testId = ID_TRANSACTION_CAP
        addClassName("jvmguard-nowrap-label")
        min = 1
        width = "10rem"
    }
    private val capStatus = Badge().apply { testId = ID_CAP_STATUS }
    private val resetCap = Button(t("settings.data.resetCap")) { resetCaps() }

    init {
        val capRow = HorizontalLayout(transactionCap, capStatus).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
            style.set("gap", "0.75rem")
        }
        add(settingsSection(t("nav.settings.data"), transactions, indefinitely, violations, snapshotDays, capRow, resetCap))
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
            .asRequired(t("settings.data.validation.enterDays"))
            .bind({ it.snapshotFileDays }, { config, value -> config.snapshotFileDays = value })
        binder.forField(transactionCap)
            .asRequired(t("settings.data.validation.enterMaximum"))
            .bind({ it.transactionCap }, { config, value -> config.transactionCap = value })
    }

    private fun refreshCapStatus() {
        val reached = Sessions.current()?.serverConnection?.caps?.contains(CapType.TRANSACTION) == true
        capStatus.removeThemeVariants(BadgeVariant.WARNING)
        if (reached) {
            capStatus.text = t("settings.data.capReached")
            capStatus.icon = VaadinIcon.WARNING.create()
            capStatus.addThemeVariants(BadgeVariant.WARNING)
        } else {
            capStatus.text = t("settings.data.capNotReached")
            capStatus.icon = null
        }
    }

    private fun resetCaps() {
        Sessions.current()?.serverConnection?.resetCaps()
        refreshCapStatus()
        Notifications.show(t("settings.data.capReset"))
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
                addValueChangeListener { setLabel(t("settings.data.days", label, (it.value ?: min.toDouble()).toInt())) }
            }
    }
}
