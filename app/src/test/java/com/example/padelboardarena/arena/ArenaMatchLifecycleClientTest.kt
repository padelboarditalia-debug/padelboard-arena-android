package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ArenaMatchLifecycleClientTest {
    @Test
    fun finishPayloadContainsScoreAndPersistentSequence() {
        val snapshot =
            ArenaMatchLifecycleScoreSnapshot(
                eventId = "finish-event",
                eventSequence = 42,
                gamesA = 6,
                gamesB = 4,
                setsA = 1,
                setsB = 0
            )

        assertEquals(
            "{" +
                    "\"eventId\":\"finish-event\"," +
                    "\"eventSequence\":42," +
                    "\"gamesA\":6," +
                    "\"gamesB\":4," +
                    "\"setsA\":1," +
                    "\"setsB\":0" +
                    "}",
            snapshot.finishJson()
        )
    }

    @Test
    fun reopenPayloadContainsMatchIdAndPersistentSequence() {
        val request =
            ArenaMatchReopenRequest(
                eventId = "reopen-event",
                eventSequence = 43,
                matchId = "match-1"
            )

        assertEquals(
            "{" +
                    "\"eventId\":\"reopen-event\"," +
                    "\"eventSequence\":43," +
                    "\"matchId\":\"match-1\"" +
                    "}",
            request.toJson()
        )
    }

    @Test
    fun lifecycleUrlsUseConfiguredBaseUrlAndCourtId() {
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider =
                    LifecycleStaticTokenProvider(
                        accessToken = "access"
                    ),
                transport = LifecycleFakeTransport()
            )

        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts/court-1/finish-match",
            client.finishMatchUrl()
        )
        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts/court-1/reopen-match",
            client.reopenMatchUrl()
        )
    }

    @Test
    fun finishUnauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            LifecycleFakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = lifecycleBody(
                        status = "finished"
                    )
                )
            )
        val tokenProvider =
            LifecycleCountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.finishMatchBlocking(
                sampleFinishSnapshot()
            )

        assertTrue(result.success)
        assertEquals("finished", result.status)
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
    }

    @Test
    fun reopenUnauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            LifecycleFakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = lifecycleBody(
                        status = "reopened"
                    )
                )
            )
        val tokenProvider =
            LifecycleCountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.reopenMatchBlocking(
                ArenaMatchReopenRequest(
                    eventId = "reopen-event",
                    eventSequence = 43,
                    matchId = "match-1"
                )
            )

        assertTrue(result.success)
        assertEquals("reopened", result.status)
        assertEquals(1, tokenProvider.refreshCount)
        assertEquals(2, transport.requests.size)
        assertTrue(
            transport.requests[1].body.contains(
                "\"matchId\":\"match-1\""
            )
        )
    }

    @Test
    fun timeoutReturnsControlledErrorWithoutCrash() {
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider =
                    LifecycleStaticTokenProvider(
                        accessToken = "access"
                    ),
                transport = LifecycleThrowingTransport()
            )

        val result =
            client.finishMatchBlocking(
                sampleFinishSnapshot()
            )

        assertFalse(result.success)
        assertEquals(0, result.statusCode)
        assertEquals("network_error", result.body)
    }

    @Test
    fun finishResultPreservesFullHttpBody() {
        val body =
            "{" +
                    "\"ok\":false," +
                    "\"status\":\"manual_override_conflict\"," +
                    "\"reason\":\"official score differs\"" +
                    "}"
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider =
                    LifecycleStaticTokenProvider(
                        accessToken = "access"
                    ),
                transport =
                    LifecycleFakeTransport(
                        ArenaHttpResponse(
                            statusCode = 409,
                            body = body
                        )
                    )
            )

        val result =
            client.finishMatchBlocking(
                sampleFinishSnapshot()
            )

        assertFalse(result.success)
        assertEquals(409, result.statusCode)
        assertEquals("manual_override_conflict", result.status)
        assertEquals(body, result.body)
    }

    @Test
    fun reopenResultPreservesFullHttpBody() {
        val body =
            "{" +
                    "\"ok\":false," +
                    "\"status\":\"sequence_conflict\"," +
                    "\"reason\":\"stale lifecycle request\"" +
                    "}"
        val client =
            ArenaMatchLifecycleClient(
                config = sampleConfig(),
                tokenProvider =
                    LifecycleStaticTokenProvider(
                        accessToken = "access"
                    ),
                transport =
                    LifecycleFakeTransport(
                        ArenaHttpResponse(
                            statusCode = 409,
                            body = body
                        )
                    )
            )

        val result =
            client.reopenMatchBlocking(
                ArenaMatchReopenRequest(
                    eventId = "reopen-event",
                    eventSequence = 43,
                    matchId = "match-1"
                )
            )

        assertFalse(result.success)
        assertEquals(409, result.statusCode)
        assertEquals("sequence_conflict", result.status)
        assertEquals(body, result.body)
    }

    @Test
    fun successfulResponseParsesMinimalMatch() {
        val result =
            ArenaMatchLifecycleResult.fromHttp(
                statusCode = 200,
                body = lifecycleBody(
                    status = "finished"
                )
            )

        assertTrue(result.success)
        assertEquals("finished", result.status)
        assertNotNull(result.match)
        assertEquals("match-1", result.match?.matchId)
        assertEquals(6, result.match?.gamesA)
        assertEquals(4, result.match?.gamesB)
        assertEquals(1, result.match?.setsA)
        assertEquals(0, result.match?.setsB)
        assertEquals("6-4", result.match?.score)
        assertEquals("finished", result.match?.matchStatus)
    }

    private fun sampleFinishSnapshot(): ArenaMatchLifecycleScoreSnapshot {
        return ArenaMatchLifecycleScoreSnapshot(
            eventId = "finish-event",
            eventSequence = 42,
            gamesA = 6,
            gamesB = 4,
            setsA = 1,
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

    private fun lifecycleBody(
        status: String
    ): String {
        return "{" +
                "\"ok\":true," +
                "\"status\":\"$status\"," +
                "\"match\":{" +
                "\"matchId\":\"match-1\"," +
                "\"gamesA\":6," +
                "\"gamesB\":4," +
                "\"setsA\":1," +
                "\"setsB\":0," +
                "\"score\":\"6-4\"," +
                "\"matchStatus\":\"finished\"" +
                "}" +
                "}"
    }
}

private class LifecycleFakeTransport(
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

private class LifecycleStaticTokenProvider(
    private val accessToken: String
) : ArenaTokenProvider {
    override fun currentAccessToken(): String {
        return accessToken
    }

    override fun refreshAccessToken(): String? {
        return null
    }
}

private class LifecycleCountingTokenProvider(
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

private class LifecycleThrowingTransport : ArenaHttpTransport {
    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        throw SocketTimeoutException(
            "failed to connect"
        )
    }
}
