package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.agent.config.recording.RetransformationType
import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.recording.MappedTransactionDefDialog
import dev.jvmguard.ui.components.recording.DeclaredTransactionDefDialog
import dev.jvmguard.ui.components.recording.MatchedTransactionDefDialog
import dev.jvmguard.ui.components.recording.sets.SaveSetDialog
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordingTransactionsTest : JvmGuardBrowserlessTest() {

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

    private fun rootDefs() =
        Sessions.recordingDraft().groupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)!!.transactionSettings.transactionDefs

    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    @Test
    fun rootShowsTransactionGridsAndRetransformation() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        assertEquals(3, find<TabSheet>().single().tabCount)
        assertTrue(find<EnumSelect<*>>().all().any { it.label == "Reinstrument classes" })
    }

    @Test
    fun addingAMatchedDefPersistsToTheGroup() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        use(find<TabSheet>().single()).select(2)
        use(find<Button>().all().first { it.text == "Add transaction" }).click()

        val dialog = find<MatchedTransactionDefDialog>().single()
        use(find<TextField>(dialog).all().first { it.label == "Class or interface name" }).setValue("com.example.Service")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(rootDefs().any { it is MatchedTransactionDef && it.declaringClassName == "com.example.Service" })

        use(shellSave()).click()
        val saved = connection.groupConfigs.first { it.isRoot }.transactionSettings.transactionDefs
        assertTrue(saved.any { it is MatchedTransactionDef && it.declaringClassName == "com.example.Service" })
    }

    @Test
    fun matchedMethodFieldsAppearOnlyForMethodInterception() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        use(find<TabSheet>().single()).select(2)
        use(find<Button>().all().first { it.text == "Add transaction" }).click()
        val dialog = find<MatchedTransactionDefDialog>().single()

        assertFalse(find<TextField>(dialog).all().any { it.label == "Method name" })

        @Suppress("UNCHECKED_CAST")
        val target = find<Select<*>>(dialog).all().first { it.label == "Intercept" } as Select<MatchedTransactionDef.InterceptionTarget>
        target.value = MatchedTransactionDef.InterceptionTarget.METHOD
        assertTrue(find<TextField>(dialog).all().any { it.label == "Method name" })
    }

    @Test
    fun addingADeclaredDefPersistsToTheGroup() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        use(find<TabSheet>().single()).select(0)
        use(find<Button>().all().first { it.text == "Add transaction" }).click()

        val dialog = find<DeclaredTransactionDefDialog>().single()
        use(find<TextField>(dialog).all().first { it.label.orEmpty().startsWith("Restrict to group") }).setValue("checkout")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(rootDefs().any { it is DeclaredTransactionDef && it.group.value == "checkout" })
    }

    @Test
    fun addingACustomAnnotationDefPersistsToTheGroup() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        use(find<TabSheet>().single()).select(1)
        use(find<Button>().all().first { it.text == "Add transaction" }).click()

        val dialog = find<MappedTransactionDefDialog>().single()
        use(find<TextField>(dialog).all().first { it.label == "Annotation class name" }).setValue("com.example.Traced")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(rootDefs().any { it is MappedTransactionDef && it.annotationName == "com.example.Traced" })
    }

    @Test
    fun savingATransactionSetStoresItOnTheServer() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        use(find<TabSheet>().single()).select(2)
        use(find<Button>().all().first { it.text == "Add transaction" }).click()
        val dialog = find<MatchedTransactionDefDialog>().single()
        use(find<TextField>(dialog).all().first { it.label == "Class or interface name" }).setValue("com.example.Service")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        use(find<Button>().all().first { it.text == "Save set" }).click()
        val setDialog = find<SaveSetDialog<*, *>>().single()
        use(find<TextField>(setDialog).all().first()).setValue("My transactions")
        use(find<Button>(setDialog).all().first { it.text == "Save" }).click()

        assertTrue(connection.transactionDefSets.any { it.name == "My transactions" && it.items.isNotEmpty() })
    }

    @Test
    fun retransformationTypePersists() {
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)

        @Suppress("UNCHECKED_CAST")
        val select = find<EnumSelect<*>>().all().first { it.label == "Reinstrument classes" } as EnumSelect<RetransformationType>
        use(select).selectItem("No retransformation, use the previous configuration at startup")

        use(shellSave()).click()
        assertEquals(RetransformationType.STARTUP, connection.groupConfigs.first { it.isRoot }.transactionSettings.retransformationType)
    }
}
