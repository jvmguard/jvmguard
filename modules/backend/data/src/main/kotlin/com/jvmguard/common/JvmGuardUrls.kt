package com.jvmguard.common

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

object JvmGuardUrls {

    const val REDIR_URL_SPEC = "https://jvmguard.dev"
    val CHANGELOG_URL: URL = makeURL("$REDIR_URL_SPEC/changes") //TODO change to GH release notes, also change in install4j project file
    val DOWNLOAD_URL: URL = makeURL("$REDIR_URL_SPEC/download")

    private fun makeURL(urlString: String): URL {
        try {
            return URI(urlString).toURL()
        } catch (_: MalformedURLException) {
            throw RuntimeException("Invalid URL $urlString")
        } catch (_: URISyntaxException) {
            throw RuntimeException("Invalid URL $urlString")
        }
    }
}
