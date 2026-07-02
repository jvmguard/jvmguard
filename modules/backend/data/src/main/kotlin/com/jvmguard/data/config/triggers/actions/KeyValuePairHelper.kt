package com.jvmguard.data.config.triggers.actions

import com.install4j.runtime.beans.KeyValuePair
import java.util.regex.Pattern

object KeyValuePairHelper {

    private val PATTERN_KEY_VALUE_PAIR: Pattern = Pattern.compile("([^=]+)=(.+)")

    fun isKeyValuePairs(text: String): Boolean =
        text.lineSequence()
            .filter { it.trim().isNotEmpty() }
            .all { PATTERN_KEY_VALUE_PAIR.matcher(it).matches() }

    fun parseKeyValuePairs(text: String): List<KeyValuePair> =
        text.lineSequence()
            .filter { it.trim().isNotEmpty() }
            .map { line ->
                val matcher = PATTERN_KEY_VALUE_PAIR.matcher(line)
                require(matcher.matches()) { "The line \"$line\" is not a key-value pair" }
                KeyValuePair(matcher.group(1), matcher.group(2))
            }
            .toList()
}
