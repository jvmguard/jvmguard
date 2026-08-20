package dev.jvmguard.ui.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.text.MessageFormat
import java.util.Properties

class I18nBundleParityTest {

    private val bundleSuffixes = listOf("", "_ko", "_ja", "_zh_CN")

    private fun load(suffix: String): Properties {
        val name = "vaadin-i18n/translations$suffix.properties"
        val resource = javaClass.classLoader.getResource(name)
        assertNotNull(resource, "missing bundle $name")
        return resource!!.openStream().reader(Charsets.UTF_8).use { Properties().apply { load(it) } }
    }

    @Test
    fun keyParityAcrossLocales() {
        val reference = load("").stringPropertyNames()
        bundleSuffixes.forEach { suffix ->
            assertEquals(reference, load(suffix).stringPropertyNames(), "key set of translations$suffix.properties")
        }
    }

    @Test
    fun placeholderParityAcrossLocales() {
        val reference = load("")
        bundleSuffixes.drop(1).forEach { suffix ->
            val translated = load(suffix)
            reference.stringPropertyNames().forEach { key ->
                assertEquals(
                    placeholders(reference.getProperty(key)),
                    placeholders(translated.getProperty(key)),
                    "placeholders of '$key' in translations$suffix.properties",
                )
            }
        }
    }

    @Test
    fun allPatternsCompileAsMessageFormat() {
        bundleSuffixes.forEach { suffix ->
            load(suffix).stringPropertyNames().forEach { key ->
                MessageFormat(load(suffix).getProperty(key)) // throws on a malformed pattern
            }
        }
    }

    // Every pattern goes through MessageFormat (even without params), so a literal apostrophe must be doubled
    @Test
    fun noUndoubledApostrophes() {
        bundleSuffixes.forEach { suffix ->
            load(suffix).stringPropertyNames().forEach { key ->
                val value = load(suffix).getProperty(key)
                assertEquals(-1, value.replace("''", "").indexOf('\''), "raw apostrophe in '$key' (translations$suffix.properties)")
            }
        }
    }

    // Compares argument-index sets, not raw format tokens: a translation may legitimately drop an
    // English `{n,choice,...}` wrapper (ko/ja/zh_CN have no plural) as long as it uses the same arguments.
    private fun placeholders(pattern: String): List<String> =
        PLACEHOLDER_REGEX.findAll(pattern).map { it.groupValues[1] }.distinct().sorted().toList()

    private companion object {
        val PLACEHOLDER_REGEX = Regex("""\{(\d+)""")
    }
}
