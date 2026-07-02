package com.jvmguard.ui.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * A VIEWER denied the ADMIN-only `triggerBackup` returns 403 via `AccessDeniedException`, proving
 * method security fired rather than a 401 or a 500 from the generic exception handler.
 */
@Tag("e2e")
class RestAuthzE2ETest : PlaywrightE2ETest() {

    @Test
    fun restApiEnforcesAccessLevelPerEndpoint() {
        controlCommand("createUser&accessLevel=ADMIN&name=$REST_ADMIN&password=$PASSWORD&apiKey=$ADMIN_KEY")
        controlCommand("createUser&accessLevel=VIEWER&name=$REST_VIEWER&password=$PASSWORD&apiKey=$VIEWER_KEY")

        assertEquals(401, apiStatus("/api/vms"), "no credentials must be rejected")
        assertEquals(401, apiStatus("/api/vms", REST_VIEWER, "wrong-key"), "an invalid API key must be rejected")

        assertEquals(200, apiStatus("/api/vms", REST_VIEWER, VIEWER_KEY), "a VIEWER may read")
        assertEquals(200, apiStatus("/api/vms", REST_ADMIN, ADMIN_KEY), "an ADMIN may read")

        assertEquals(403, apiStatus("/api/triggerBackup", REST_VIEWER, VIEWER_KEY), "a VIEWER must be denied the ADMIN-only backup")
        assertEquals(200, apiStatus("/api/triggerBackup", REST_ADMIN, ADMIN_KEY), "an ADMIN may trigger a backup")
    }

    private companion object {
        const val REST_ADMIN = "restadmin"
        const val REST_VIEWER = "restviewer"
        const val PASSWORD = "restpass123"
        const val ADMIN_KEY = "admin-api-key-0123456789"
        const val VIEWER_KEY = "viewer-api-key-0123456789"
    }
}
