package com.jvmguard.ui.views.vms

import com.jvmguard.agent.config.VmType
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.viewsettings.SparkLineScaleMode
import com.jvmguard.data.vmdata.SparkLineRange
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VmDataHolder
import com.jvmguard.data.vmdata.VmFilter
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VmTreeGridTest : JvmGuardBrowserlessTest() {

    /** Feeds a scripted sequence of VM trees so successive reloads see a changing hierarchy. */
    private class ScriptedConnection(base: ServerConnection, private val trees: List<Group<VmDataHolder>>) :
        ServerConnection by base {
        private var index = 0
        override fun getVmDataHolders(
            vmFilter: VmFilter,
            sparkLineRange: SparkLineRange,
            telemetryTypes: Collection<TelemetryType>,
        ): Group<VmDataHolder> = trees[minOf(index++, trees.lastIndex)]
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    /** A root Group whose children are the named groups, each given a child so it is expandable. */
    private fun treeOf(vararg rootNames: String): Group<VmDataHolder> {
        val root = Group<VmDataHolder>()
        for (name in rootNames) {
            root.getOrCreateGroupChild(VmIdentifier(name, VmType.GROUP))
                .getOrCreateGroupChild(VmIdentifier("$name/leaf", VmType.GROUP))
        }
        return root
    }

    private fun gridFedWith(vararg trees: Group<VmDataHolder>): VmTreeGrid {
        Sessions.setCurrent(UserSession(ScriptedConnection(MockConnections.create(AccessLevel.ADMIN), trees.toList())))
        return VmTreeGrid().also { UI.getCurrent().add(it) }
    }

    private fun VmTreeGrid.reloadDefault() =
        reload(VmFilter.CONNECTED, SparkLineRange.LAST_HOUR, SparkLineScaleMode.SEPARATE)

    // VmTreeItem equality is key-based, so a fresh item with the same key addresses the real one in the grid.
    private fun group(name: String) = VmGroupItem(name, name, VmType.GROUP, null)

    @Test
    fun aGroupThatConnectsAfterTheFirstReloadStillGetsTheDefaultExpansion() {
        // "B" is present at login; "A" (alphabetically first) shows up only on the second reload.
        val grid = gridFedWith(treeOf("B"), treeOf("A", "B"))
        grid.reloadDefault()
        grid.reloadDefault()
        assertTrue(grid.isExpanded(group("A")), "a group appearing after login must get the default expansion")
    }

    @Test
    fun aGroupTheUserCollapsedIsNotReExpandedOnReload() {
        val grid = gridFedWith(treeOf("B"), treeOf("B"))
        grid.reloadDefault()
        assertTrue(grid.isExpanded(group("B")), "the first group is expanded by default")

        grid.collapse(group("B"))
        grid.reloadDefault()
        assertFalse(grid.isExpanded(group("B")), "a user-collapsed group must stay collapsed across reloads")
    }
}
