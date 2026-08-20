package dev.jvmguard.data.user.viewsettings

import java.io.Serializable

open class ViewSettings : Serializable {
    var vmPanelSettings: VmPanelSettings = VmPanelSettings()

    /** BCP-47 language tag ("en", "ko", "ja", "zh-CN")
     *  empty means auto-detect from Accept-Language
     */
    var locale: String = ""
}
