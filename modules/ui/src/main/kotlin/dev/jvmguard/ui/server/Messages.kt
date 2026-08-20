package dev.jvmguard.ui.server

import dev.jvmguard.common.LocalizableMessage
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinService
import java.text.MessageFormat
import java.util.ResourceBundle

private val ENGLISH_BUNDLE: ResourceBundle by lazy {
    ResourceBundle.getBundle(
        "vaadin-i18n/translations", Locales.ENGLISH, Locales::class.java.classLoader,
        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES),
    )
}

fun t(key: String, vararg params: Any?): String {
    UI.getCurrent()?.let { return it.getTranslation(key, *params) }
    // Background threads have no UI, so fall back to English
    VaadinService.getCurrent()?.instantiator?.i18NProvider?.let { return it.getTranslation(key, Locales.ENGLISH, *params) }
    // No Vaadin environment at all, read the English bundle directly
    val pattern = runCatching { ENGLISH_BUNDLE.getString(key) }.getOrNull() ?: return key
    return MessageFormat(pattern, Locales.ENGLISH).format(params)
}

fun <T : Enum<T>> enumLabel(e: T): String = t("enum.${e.declaringJavaClass.simpleName}.${e.name}")

fun errorText(e: Throwable): String =
    (e as? LocalizableMessage)?.let { t(it.messageKey, *displayParams(it.messageParams)) }
        ?: (e.message ?: e.javaClass.name)

fun mbeanErrorText(errorKey: String?, errorParams: Array<Any>, fallback: String?): String? =
    errorKey?.let { t(it, *displayParams(errorParams)) } ?: fallback

private fun displayParams(params: Array<out Any?>): Array<Any?> =
    params.map { if (it is Enum<*>) t("enum.${it.declaringJavaClass.simpleName}.${it.name}") else it }.toTypedArray()
