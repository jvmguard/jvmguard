package com.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.jvmguard.ui.components.recording.triggers.TriggerActionDialog
import com.jvmguard.ui.shell.AddLocalVmsDialog
import com.jvmguard.ui.shell.AddVmsLocationDialog
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Test

class VmActionsScreenshots : ScreenshotTest() {

    @Test
    fun addVmsLocal() = onPage {
        login()
        getByTestId(MainLayout.ID_ADD_VMS).click()
        getByTestId(AddVmsLocationDialog.ID_THIS_MACHINE).click()
        assertThat(getByTestId(AddLocalVmsDialog.ID_CLOSE)).isVisible()
        capture("add_vms_local")
    }

    @Test
    fun vmsProfilingAction() = onPage {
        login()
        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        assertThat(getByTestId(VmTreeGrid.ID_ACTION_RECORD_JPS)).isVisible()
        capture("vms_profiling_action")
    }

    @Test
    fun jfrRecordingAction() = onPage {
        login()
        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        assertThat(getByTestId(VmTreeGrid.ID_ACTION_RECORD_JFR)).isVisible()
        capture("jfr_recording_action")
    }

    @Test
    fun profilingOptions() = onPage {
        login()
        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        getByTestId(VmTreeGrid.ID_ACTION_RECORD_JPS).click()
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("profiling_options")
    }

    @Test
    fun jfrRecordingOptions() = onPage {
        login()
        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        getByTestId(VmTreeGrid.ID_ACTION_RECORD_JFR).click()
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        capture("jfr_recording_options")
    }
}
