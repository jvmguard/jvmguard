package com.jvmguard.server

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IpAllowlistTest {

    @Test
    fun emptyAllowlistPermitsAnyAddress() {
        assertTrue(IpAllowlist.isAllowed("203.0.113.7", ""))
        assertTrue(IpAllowlist.isAllowed("::1", "   "))
        assertTrue(IpAllowlist.isAllowed(null, ""))
    }

    @Test
    fun matchesExactAndCidrIpv4() {
        assertTrue(IpAllowlist.isAllowed("10.1.2.3", "10.0.0.0/8, 192.168.1.10"))
        assertTrue(IpAllowlist.isAllowed("192.168.1.10", "10.0.0.0/8, 192.168.1.10"))
        assertFalse(IpAllowlist.isAllowed("203.0.113.7", "10.0.0.0/8, 192.168.1.10"))
    }

    @Test
    fun matchesIpv6ExactAndCidr() {
        assertTrue(IpAllowlist.isAllowed("::1", "::1"))
        assertTrue(IpAllowlist.isAllowed("2001:db8::5", "2001:db8::/32"))
        assertFalse(IpAllowlist.isAllowed("2001:dead::5", "2001:db8::/32"))
    }

    @Test
    fun nonEmptyAllowlistRejectsUnknownAddressAndNull() {
        assertFalse(IpAllowlist.isAllowed(null, "10.0.0.0/8"))
    }

    @Test
    fun malformedEntriesAreIgnoredButValidOnesStillMatch() {
        assertTrue(IpAllowlist.isAllowed("10.1.2.3", "not-an-ip, 10.0.0.0/8"))
        // A list with only malformed entries fails closed.
        assertFalse(IpAllowlist.isAllowed("10.1.2.3", "not-an-ip, also-bad"))
    }

    @Test
    fun ipv4MappedIpv6MatchesAnIpv4Entry() {
        assertTrue(IpAllowlist.isAllowed("::ffff:10.1.2.3", "10.0.0.0/8"))
        assertTrue(IpAllowlist.isAllowed("::ffff:192.168.1.10", "192.168.1.10"))
    }

    @Test
    fun loopbackFormsAreEquivalent() {
        // An admin who allowlists either loopback form should not lock out the other.
        assertTrue(IpAllowlist.isAllowed("::1", "127.0.0.1"))
        assertTrue(IpAllowlist.isAllowed("0:0:0:0:0:0:0:1", "127.0.0.1"))
        assertTrue(IpAllowlist.isAllowed("127.0.0.1", "::1"))
    }
}
