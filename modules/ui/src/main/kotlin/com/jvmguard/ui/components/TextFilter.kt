package com.jvmguard.ui.components

fun nameMatchesFilter(name: String, query: String, useRegex: Boolean, matchCase: Boolean): Boolean {
    if (query.isEmpty()) {
        return true
    }
    return if (useRegex) {
        runCatching {
            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            query.toRegex(options).containsMatchIn(name)
        }.getOrDefault(false)
    } else {
        name.contains(query, ignoreCase = !matchCase)
    }
}
