package dev.jvmguard.common.util

object StringUtil {

    fun countChar(c: Char, string: String): Int {
        var count = 0
        var fromIndex = -1
        while (string.indexOf(c, fromIndex + 1).also { fromIndex = it } > -1) {
            count++
        }
        return count
    }

    fun capitalizeFirstLetter(string: String?): String? {
        if (string == null) {
            return null
        }
        return if (string[0].isUpperCase()) {
            string
        } else {
            string[0].uppercaseChar() + string.substring(1)
        }
    }
}
