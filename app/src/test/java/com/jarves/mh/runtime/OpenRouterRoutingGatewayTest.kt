package com.jarves.mh.runtime

import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import java.net.Socket
import java.net.URI
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterRoutingGatewayTest {
    @Test
    fun routingPolicyIsAddedWithoutChangingTheRequest() {
        val source = JSONObject()
            .put("model", "anthropic/claude-sonnet-4.6")
            .put("stream", true)
        val profile = ProviderProfile(
            kind = ProviderKind.LLM_ROUTER,
            openRouterProviderOrder = "anthropic, amazon-bedrock, anthropic",
            openRouterAllowFallbacks = false,
        )

        val routed = applyOpenRouterRouting(source, profile)

        assertEquals("anthropic/claude-sonnet-4.6", routed.getString("model"))
        assertEquals(true, routed.getBoolean("stream"))
        val policy = routed.getJSONObject("provider")
        assertEquals(listOf("anthropic", "amazon-bedrock"), policy.getJSONArray("order").let { array ->
            (0 until array.length()).map(array::getString)
        })
        assertFalse(policy.getBoolean("allow_fallbacks"))
    }

    @Test
    fun blankProviderOrderLeavesAutomaticRoutingUntouched() {
        val source = JSONObject().put("model", "auto")

        applyOpenRouterRouting(source, ProviderProfile(ProviderKind.LLM_ROUTER))

        assertFalse(source.has("provider"))
    }

    @Test
    fun rejectsLoopbackRequestsWithoutTheConfiguredKey() {
        // A co-installed app can reach the loopback port; without an auth gate it could have its
        // requests forwarded upstream on the user's key. The gateway must reject anything that does
        // not present the configured key before it ever contacts the provider.
        OpenRouterRoutingGateway(ProviderProfile(ProviderKind.LLM_ROUTER), apiKey = "secret-key")
            .start()
            .use { gateway ->
                val uri = URI(gateway.url)
                Socket(uri.host, uri.port).use { socket ->
                    socket.getOutputStream().apply {
                        write(
                            (
                                "POST /v1/messages HTTP/1.1\r\n" +
                                    "Host: 127.0.0.1\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "\r\n"
                                ).toByteArray(),
                        )
                        flush()
                    }
                    val status = socket.getInputStream().bufferedReader().readLine().orEmpty()
                    assertTrue("expected 401, got: $status", status.contains("401"))
                }
            }
    }
}
