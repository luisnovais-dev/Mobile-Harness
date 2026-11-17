package com.jarves.mh.network

import java.net.InetAddress
import java.net.URI

/**
 * Plain-HTTP policy for user-entered provider URLs.
 *
 * Self-hosted model servers (llama.cpp, LM Studio, Ollama, Strata…) usually speak plain HTTP on the
 * local network or over Tailscale, so the platform config permits cleartext. This check keeps API
 * traffic encrypted everywhere else: http:// is only accepted for loopback, private, link-local and
 * Tailscale addresses, and for local-only host names.
 */
object LocalNetwork {
    private val localSuffixes = listOf(".local", ".lan", ".home", ".home.arpa", ".internal", ".localdomain", ".ts.net")

    /** Resolver seam so the DNS-rebinding guard can be exercised without real lookups in tests. */
    internal fun interface HostResolver {
        /** Every address [host] resolves to, or an empty list when resolution fails. */
        fun resolve(host: String): List<InetAddress>
    }

    private val systemResolver = HostResolver { host ->
        runCatching { InetAddress.getAllByName(host).toList() }.getOrDefault(emptyList())
    }

    fun isPlainHttpAllowed(url: String): Boolean = isPlainHttpAllowed(url, systemResolver)

    internal fun isPlainHttpAllowed(url: String, resolver: HostResolver): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("http", ignoreCase = true)) return true
        val host = uri.host ?: return false
        if (!isLocalHost(host)) return false
        // Defense-in-depth against DNS rebinding: a name we accepted as "local" must not actually
        // resolve to a public address, or the API key would travel there in cleartext. Fail open
        // when resolution fails (offline / mDNS not answering), fail closed on any public result.
        val resolved = resolver.resolve(host)
        if (resolved.isNotEmpty() && resolved.any { !isLocalAddress(it) }) return false
        return true
    }

    fun isLocalHost(rawHost: String): Boolean {
        val host = rawHost.trim().trim('[', ']').lowercase().trimEnd('.')
        if (host.isEmpty()) return false
        if (host == "localhost" || localSuffixes.any(host::endsWith)) return true
        if (host.contains(':')) return isLocalIpv6(host)
        val octets = host.split('.')
        if (octets.size == 4 && octets.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            return isPrivateIpv4(octets[0].toInt(), octets[1].toInt())
        }
        // A dotless packed numeric literal is an IP in disguise, not a host name
        // (2130706433 == 127.0.0.1, but 134744072 == 8.8.8.8): range-check it, never trust it.
        packedIpv4(host)?.let { packed ->
            return isPrivateIpv4((packed ushr 24 and 0xFF).toInt(), (packed ushr 16 and 0xFF).toInt())
        }
        // A genuine single-label name (e.g. "my-pc") can only resolve on the local network.
        return !host.contains('.')
    }

    private fun isPrivateIpv4(a: Int, b: Int): Boolean =
        a == 10 || a == 127 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254) ||
            (a == 100 && b in 64..127) // Tailscale / carrier-grade NAT

    /** Parse a dotless packed IPv4 literal (decimal `2130706433` or hex `0x7f000001`); else null. */
    private fun packedIpv4(host: String): Long? {
        val value = when {
            host.startsWith("0x") -> host.drop(2).toLongOrNull(16)
            host.all(Char::isDigit) -> host.toLongOrNull()
            else -> null
        } ?: return null
        return value.takeIf { it in 0..0xFFFFFFFFL }
    }

    private fun isLocalAddress(address: InetAddress): Boolean =
        address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress ||
            isLocalHost(address.hostAddress ?: return false)

    private fun isLocalIpv6(host: String): Boolean =
        host == "::1" || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe8") ||
            host.startsWith("fe9") || host.startsWith("fea") || host.startsWith("feb")
}
