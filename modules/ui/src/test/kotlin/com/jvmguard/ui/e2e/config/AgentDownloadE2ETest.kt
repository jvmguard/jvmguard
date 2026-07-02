package com.jvmguard.ui.e2e.config

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.jvmguard.ui.shell.AddLocalVmsDialog
import com.jvmguard.ui.shell.AddVmsLocationDialog
import com.jvmguard.ui.shell.MainLayout
import org.junit.jupiter.api.Test

/**
 * The E2E server is on localhost, so "Add VMs" opens the location dialog and "This machine" shows the
 * local-agent -javaagent instructions; only the remote-machine path exposes an actual agent download.
 */
class AgentDownloadE2ETest : ConfigE2ETest() {

    @Test
    fun thisMachineShowsLocalAgentInstructions() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        getByTestId(MainLayout.ID_ADD_VMS).click()
        assertThat(getByTestId(AddVmsLocationDialog.ID_THIS_MACHINE)).isVisible()
        getByTestId(AddVmsLocationDialog.ID_THIS_MACHINE).click()

        assertThat(getByTestId(AddLocalVmsDialog.ID_CLOSE)).isVisible()
    }
}
