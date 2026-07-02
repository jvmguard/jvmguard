package com.jvmguard.ui.server

import com.jvmguard.common.notification.ModificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserSessionTest {

    @Test
    fun pollAndDispatch_deliversModificationsToListeners() {
        val mock = MockConnections.create()
        mock.modified(ModificationType.INBOX)
        val session = UserSession(mock)

        val received = mutableListOf<Set<ModificationType>>()
        session.addModificationListener { received.add(it) }

        val polled = session.pollAndDispatch()

        assertTrue(polled)
        assertEquals(1, received.size)
        assertTrue(received.first().contains(ModificationType.INBOX))
    }

    @Test
    fun pollAndDispatch_doesNotNotifyWhenNothingChanged() {
        val session = UserSession(MockConnections.create())

        val received = mutableListOf<Set<ModificationType>>()
        session.addModificationListener { received.add(it) }

        session.pollAndDispatch()

        assertTrue(received.isEmpty())
    }
}
