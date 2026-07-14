package dev.jvmguard.server

import org.springframework.security.web.util.matcher.IpAddressMatcher
import java.net.InetAddress

object IpAllowlist {

    fun isAllowed(remoteAddr: String?, allowlistCsv: String): Boolean {
        val entries = allowlistCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (entries.isEmpty()) {
            return true
        }
        if (remoteAddr.isNullOrBlank()) {
            return false
        }
        val candidates = candidateForms(remoteAddr)
        return entries.any { entry -> candidates.any { matches(entry, it) } }
    }

    // On a dual-stack listener the same client can arrive in several equivalent forms: an
    // IPv4-mapped IPv6 address (::ffff:10.1.2.3), or IPv4 vs IPv6 loopback.
    private fun candidateForms(remoteAddr: String): Set<String> {
        val forms = linkedSetOf(remoteAddr)
        try {
            val address = InetAddress.getByName(remoteAddr)
            forms.add(address.hostAddress)
            if (address.isLoopbackAddress) {
                forms.add("127.0.0.1")
                forms.add("::1")
            }
        } catch (_: Exception) {
            // fall back to the raw string
        }
        return forms
    }

    private fun matches(entry: String, remoteAddr: String): Boolean =
        try {
            IpAddressMatcher(entry).matches(remoteAddr)
        } catch (_: IllegalArgumentException) {
            false
        }
}
