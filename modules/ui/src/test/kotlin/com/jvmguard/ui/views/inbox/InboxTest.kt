package com.jvmguard.ui.views.inbox

import com.jvmguard.data.user.InboxItem
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.badge.Badge
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InboxTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: MockServerConnectionImpl

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create()
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    private fun openInbox() {
        UI.getCurrent().navigate(InboxView::class.java)
        assertInstanceOf(InboxView::class.java, currentView)
    }

    @Suppress("UNCHECKED_CAST")
    private fun grid(): Grid<InboxItem> = find<Grid<*>>().single() as Grid<InboxItem>
    private fun button(testId: String): Button = find<Button>().all().first { it.testId == testId }
    private fun isButtonVisible(testId: String): Boolean = find<Button>().all().any { it.testId == testId }

    @Test
    fun listsItemsAndShowsUnreadBadge() {
        openInbox()
        assertEquals(connection.inboxItems.size, use(grid()).size())

        val badge = find<Badge>().all().first { it.testId == MainLayout.ID_INBOX_BADGE }
        assertTrue(badge.isVisible)
        assertEquals(connection.inboxItems.count { !it.isItemRead }.toString(), badge.text)
    }

    @Test
    fun selectingMarksItemRead() {
        openInbox()
        val item = connection.inboxItems.first { !it.isItemRead }
        grid().select(item)
        assertTrue(item.isItemRead)
    }

    @Test
    fun readingItemUpdatesBadgeImmediately() {
        openInbox()
        val badge = find<Badge>().all().first { it.testId == MainLayout.ID_INBOX_BADGE }
        val before = connection.inboxItems.count { !it.isItemRead }
        assertEquals(before.toString(), badge.text)

        grid().select(connection.inboxItems.first { !it.isItemRead })
        assertEquals((before - 1).toString(), badge.text)
    }

    @Test
    fun deleteMultipleButtonTogglesMode() {
        openInbox()
        val deleteButton = button(InboxView.ID_DELETE_MULTIPLE)
        assertEquals("Delete multiple", deleteButton.text)
        assertFalse(isButtonVisible(InboxView.ID_CANCEL_MULTI))

        use(deleteButton).click()
        assertEquals("Delete selected", deleteButton.text)
        assertTrue(isButtonVisible(InboxView.ID_CANCEL_MULTI))

        use(button(InboxView.ID_CANCEL_MULTI)).click()
        assertEquals("Delete multiple", deleteButton.text)
        assertFalse(isButtonVisible(InboxView.ID_CANCEL_MULTI))
    }

    @Test
    fun deleteMultipleRemovesSelectedAfterConfirm() {
        openInbox()
        val before = use(grid()).size()
        use(button(InboxView.ID_DELETE_MULTIPLE)).click()
        val targets = connection.inboxItems.take(2)
        targets.forEach { grid().select(it) }
        use(button(InboxView.ID_DELETE_MULTIPLE)).click()

        val dialog = find<ConfirmDialog>().all().first()
        ComponentUtil.fireEvent(dialog, ConfirmDialog.ConfirmEvent(dialog, false))
        assertEquals(before - 2, use(grid()).size())
    }

    @Test
    fun nameColumnHasActionsMenu() {
        openInbox()
        val cell = use(grid()).getCellComponent(0, InboxView.COL_NAME) as HorizontalLayout
        assertTrue(cell.children.anyMatch { it is Button })
    }

    @Test
    fun unreadRowsAreBold() {
        openInbox()
        val cell = use(grid()).getCellComponent(0, InboxView.COL_NAME) as HorizontalLayout
        val nameSpan = cell.children.toList().filterIsInstance<Span>().first()
        assertTrue(nameSpan.classNames.contains(InboxView.UNREAD_CLASS))
    }
}
