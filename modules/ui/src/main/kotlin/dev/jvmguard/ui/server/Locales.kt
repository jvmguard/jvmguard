package dev.jvmguard.ui.server

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import java.util.Locale

object Locales {

    val ENGLISH: Locale = Locale.ENGLISH
    val KOREAN: Locale = Locale.KOREAN
    val JAPANESE: Locale = Locale.JAPANESE
    val SIMPLIFIED_CHINESE: Locale = Locale.SIMPLIFIED_CHINESE

    val SUPPORTED: List<Locale> = listOf(ENGLISH, KOREAN, JAPANESE, SIMPLIFIED_CHINESE)

    fun tag(locale: Locale): String = locale.toLanguageTag()

    fun fromTag(tag: String): Locale? = SUPPORTED.firstOrNull { tag(it).equals(tag, ignoreCase = true) }

    /** Language names always render in their own language; they are never translated. */
    fun nativeName(locale: Locale): String = when (locale) {
        KOREAN -> "한국어"
        JAPANESE -> "日本語"
        SIMPLIFIED_CHINESE -> "简体中文"
        else -> "English"
    }

    /**
     * Applies the per-user locale override at UI init, before any route target is built. Captures the
     * browser-detected locale first so the selector can show what "Auto" resolves to.
     */
    fun initUiLocale(ui: UI) {
        ComponentUtil.setData(ui, DETECTED_LOCALE_ATTRIBUTE, ui.locale ?: ENGLISH)
        storedLocale()?.let { ui.locale = it }
    }

    /** The user's explicit choice from `ViewSettings.locale`, or null when set to auto. */
    fun storedLocale(): Locale? = Sessions.current()?.viewSettings?.locale
        ?.takeIf { it.isNotEmpty() }
        ?.let(::fromTag)

    /** What Accept-Language matching picked for this UI (before any user override was applied). */
    fun detectedLocale(ui: UI): Locale =
        ComponentUtil.getData(ui, DETECTED_LOCALE_ATTRIBUTE) as? Locale ?: ui.locale ?: ENGLISH

    private const val DETECTED_LOCALE_ATTRIBUTE = "jvmguard.detectedLocale"
}
