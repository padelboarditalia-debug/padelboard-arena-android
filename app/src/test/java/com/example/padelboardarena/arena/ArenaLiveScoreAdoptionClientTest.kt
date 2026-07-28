package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ArenaLiveScoreAdoptionClientTest {
    @Test
    fun adoptionPayloadContainsMatchScoreAndSequence() {
        val request =
            sampleRequest()

        assertEquals(
            "{" +
                    "\"eventId\":\"11111111-1111-4111-8111-111111111111\"," +
                    "\"eventSequence\":12," +
                    "\"matchId\":\"4\"," +
                    "\"gamesA\":8," +
                    "\"gamesB\":5," +
                    "\"setsA\":0," +
                    "\"setsB\":0" +
                    "}",
            request.toJson()
        )
    }

    @Test
    fun adoptionUrlUsesConfiguredCourt() {
        val client =
            ArenaLiveScoreAdoptionClient(
                config = sampleConfig(),
                tokenProvider = AdoptionStaticTokenProvider(
                    accessToken = "access"
                ),
                transport = AdoptionFakeTransport()
            )

        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts/court-1/adopt-live-score",
            client.adoptLiveScoreUrl()
        )
    }

    @Test
    fun unauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            AdoptionFakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = "{\"ok\":true,\"status\":\"adopted\"}"
                )
            )
        val tokenProvider =
            AdoptionCountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )
        val client =
            ArenaLiveScoreAdoptionClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.adoptLiveScoreBlocking(
                sampleRequest()
            )

        assertTrue(result.success)
        assertEquals("adopted", result.status)
        assertEquals(1, tokenProvider.refreshCount)
        assertEquals(2, transport.requests.size)
        assertEquals(
            "Bearer expired",
            transport.requests[0].headers["Authorization"]
        )
        assertEquals(
            "Bearer fresh",
            transport.requests[1].headers["Authorization"]
        )
        assertFalse(transport.requests[1].body.contains("fresh"))
    }

    @Test
    fun timeoutReturnsControlledError() {
        val client =
            ArenaLiveScoreAdoptionClient(
                config = sampleConfig(),
                tokenProvider = AdoptionStaticTokenProvider(
                    accessToken = "access"
                ),
                transport = AdoptionThrowingTransport()
            )

        val result =
            client.adoptLiveScoreBlocking(
                sampleRequest()
            )

        assertFalse(result.success)
        assertEquals(0, result.statusCode)
        assertEquals("network_error", result.body)
    }

    private fun sampleRequest(): ArenaLiveScoreAdoptionRequest {
        return ArenaLiveScoreAdoptionRequest(
            eventId = "11111111-1111-4111-8111-111111111111",
            eventSequence = 12,
            matchId = "4",
            gamesA = 8,
            gamesB = 5,
            setsA = 0,
            setsB = 0
        )
    }

    private fun sampleConfig(): ArenaConfig {
        return ArenaConfig(
            apiBaseUrl = "http://10.0.2.2:3000/",
            courtId = "/court-1/",
            supabaseUrl = "https://example.supabase.co",
            supabasePublishableKey = "publishable",
            email = "arena@example.com"
        )
    }
}

private class AdoptionFakeTransport(
    vararg responses: ArenaHttpResponse
) : ArenaHttpTransport {
    private val pendingResponses =
        ArrayDeque(
            responses.toList()
        )

    val requests =
        mutableListOf<ArenaHttpRequest>()

    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        requests.add(
            request
        )

        return if (pendingResponses.isEmpty()) {
            ArenaHttpResponse(
                statusCode = 500,
                body = "missing fake response"
            )
        } else {
            pendingResponses.removeFirst()
        }
    }
}

private class AdoptionStaticTokenProvider(
    private val accessToken: String
) : ArenaTokenProvider {
    override fun currentAccessToken(): String {
        return accessToken
    }

    override fun refreshAccessToken(): String? {
        return null
    }
}

private class AdoptionCountingTokenProvider(
    private val accessToken: String,
    private val refreshedToken: String
) : ArenaTokenProvider {
    var refreshCount = 0

    override fun currentAccessToken(): String {
        return accessToken
    }

    override fun refreshAccessToken(): String {
        refreshCount += 1

        return refreshedToken
    }
}

private class AdoptionThrowingTransport : ArenaHttpTransport {
    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        throw SocketTimeoutException(
            "failed to connect"
        )
    }
}
