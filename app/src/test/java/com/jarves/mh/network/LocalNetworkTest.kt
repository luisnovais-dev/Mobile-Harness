package com.jarves.mh.network

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {
    // Literal addresses do not hit DNS, so these resolvers never touch the network.
    private val noResolve = LocalNetwork.HostResolver { emptyList() }
    private fun resolvesTo(vararg ips: String) =
        LocalNetwork.HostResolver { ips.map(InetAddress::getByName) }

    @Test
    fun httpsIsAlwaysAllowed() {
        assertTrue(LocalNetwork.isPlainHttpAllowed("https://api.example.com/v1"))
    }

    @Test
    fun httpIsAllowedForLocalAndPrivateAddresses() {
        listOf(
            "http://localhost:8080",
            "http://127.0.0.1:11434/v1",
            "http://192.168.1.20:8080",
            "http://10.0.0.5:1234/v1",
            "http://172.20.3.4:8000",
            "http://100.101.102.103:8080", // Tailscale
            "http://my-pc:8080",
            "http://llama.local:8080",
            "http://box.tailnet-abc.ts.net:8080",
            "http://[::1]:8080",
            "http://[fd7a:115c:a1e0::1]:8080",
        ).forEach { assertTrue(it, LocalNetwork.isPlainHttpAllowed(it, noResolve)) }
    }

    @Test
    fun httpIsRejectedForPublicHosts() {
        listOf(
            "http://api.example.com/v1",
            "http://8.8.8.8:8080",
            "http://172.32.0.1",
            "http://100.128.0.1",
            "http://[2001:db8::1]:8080",
        ).forEach { assertFalse(it, LocalNetwork.isPlainHttpAllowed(it, noResolve)) }
    }

    @Test
    fun packedNumericLoopbackIsAllowedButPublicIsRejected() {
        // 2130706433 == 127.0.0.1 ; 0x7f000001 == 127.0.0.1 (loopback, allowed).
        assertTrue(LocalNetwork.isPlainHttpAllowed("http://2130706433/", noResolve))
        assertTrue(LocalNetwork.isPlainHttpAllowed("http://0x7f000001/", noResolve))
        // 134744072 == 8.8.8.8 ; 0x08080808 == 8.8.8.8 (public, must not pass as a "local name").
        assertFalse(LocalNetwork.isPlainHttpAllowed("http://134744072/", noResolve))
        assertFalse(LocalNetwork.isPlainHttpAllowed("http://0x08080808/", noResolve))
    }

    @Test
    fun localNameResolvingToPublicAddressIsRejected() {
        // DNS rebinding: a ".internal" name an attacker points at a public IP must not leak the key.
        assertFalse(LocalNetwork.isPlainHttpAllowed("http://nas.internal:8080", resolvesTo("8.8.8.8")))
        assertFalse(LocalNetwork.isPlainHttpAllowed("http://my-pc:8080", resolvesTo("203.0.113.7")))
    }

    @Test
    fun localNameResolvingToPrivateAddressIsAllowed() {
        assertTrue(LocalNetwork.isPlainHttpAllowed("http://nas.internal:8080", resolvesTo("192.168.1.50")))
        assertTrue(LocalNetwork.isPlainHttpAllowed("http://my-pc:8080", resolvesTo("10.0.0.9")))
    }

    @Test
    fun unresolvableLocalNameFailsOpen() {
        // Offline / mDNS not answering: keep working for legitimate local servers.
        assertTrue(LocalNetwork.isPlainHttpAllowed("http://my-pc:8080", noResolve))
    }
}
