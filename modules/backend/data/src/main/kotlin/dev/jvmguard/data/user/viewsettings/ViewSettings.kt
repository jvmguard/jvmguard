package dev.jvmguard.data.user.viewsettings

import java.io.Serializable

open class ViewSettings : Serializable {
    var vmPanelSettings: VmPanelSettings = VmPanelSettings()
}
