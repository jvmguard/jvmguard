package com.jvmguard.ui.views.settings

import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserEditDialogTest : JvmGuardBrowserlessTest() {

    private fun profilerWithAllGroups() = User().apply {
        loginName = "sso-user"
        accessLevel = AccessLevel.PROFILER
        groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
    }

    private fun openDialog(user: User, onSave: (User) -> Unit = {}): MultiSelectComboBox<String> {
        UserEditDialog(user, false, emptySet(), false, listOf("Demo/Browse", "Demo/Purchase"), onSave).open()
        @Suppress("UNCHECKED_CAST")
        return find<MultiSelectComboBox<*>>().all().first { it.label == "Associated VM groups" }
                as MultiSelectComboBox<String>
    }

    @Test
    fun rootGroupRendersAsAllGroupsInsteadOfABlankChip() {
        val groups = openDialog(profilerWithAllGroups())
        assertEquals(UserEditDialog.ALL_GROUPS_LABEL, groups.itemLabelGenerator.apply(GroupHelper.ROOT_GROUP_ID))
    }

    @Test
    fun allGroupsScopeSurvivesOpenAndSaveUnchanged() {
        var saved: User? = null
        val groups = openDialog(profilerWithAllGroups()) { saved = it }
        assertEquals(setOf(GroupHelper.ROOT_GROUP_ID), groups.value, "the all-groups scope is not silently lost")

        find<com.vaadin.flow.component.button.Button>().all().first { it.text == "Save" }.let { use(it).click() }
        assertEquals(listOf(GroupHelper.ROOT_GROUP_ID), saved?.groupNames, "saving unchanged keeps all-groups")
    }

    @Test
    fun allGroupsIsOfferedWhenConfiguringAUserManually() {
        val scoped = User().apply {
            loginName = "scoped-user"
            accessLevel = AccessLevel.PROFILER
            groupNames = arrayListOf("Demo/Browse")
        }
        val groups = openDialog(scoped)
        assertTrue(
            GroupHelper.ROOT_GROUP_ID in groups.listDataView.items.toList(),
            "the All VM groups option is offered even for a user that does not already have it",
        )
        assertTrue(
            groups.value.none { it == GroupHelper.ROOT_GROUP_ID },
            "offering the option does not select it for a scoped profiler",
        )
    }
}
