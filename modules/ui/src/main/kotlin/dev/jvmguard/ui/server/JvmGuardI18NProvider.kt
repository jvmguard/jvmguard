package dev.jvmguard.ui.server

import com.vaadin.flow.i18n.I18NProvider
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 * I18N provider, returned by `KeepAliveInstantiator.getI18NProvider()` (replacing the Spring I18NProvider).
 * Unlike the Vaadin DefaultI18NProvider, an unmatched Accept-Language falls back to English
 *
 * Every pattern goes through MessageFormat so literal apostrophes are always doubled ('') in the bundles
 */
class JvmGuardI18NProvider : I18NProvider {

    override fun getProvidedLocales(): List<Locale> = Locales.SUPPORTED

    override fun getTranslation(key: String, locale: Locale, vararg params: Any?): String {
        val bundle = ResourceBundle.getBundle(
            BUNDLE_NAME, locale, JvmGuardI18NProvider::class.java.classLoader,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES),
        )
        val pattern = try {
            bundle.getString(key)
        } catch (_: MissingResourceException) {
            LOGGER.warn("Missing translation key '{}'", key)
            return "!$key!"
        }
        return MessageFormat(pattern, locale).format(params)
    }

    companion object {
        private const val BUNDLE_NAME = "vaadin-i18n/translations"
        private val LOGGER = LoggerFactory.getLogger(JvmGuardI18NProvider::class.java)
    }
}
