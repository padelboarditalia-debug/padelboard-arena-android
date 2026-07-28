package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ArenaClientTest {
    @Test
    fun scoreSnapshotSerializesExpectedPayload() {
        val snapshot =
            sampleSnapshot()

        assertEquals(
            "{" +
                    "\"eventId\":\"uuid\"," +
                    "\"eventSequence\":1," +
                    "\"occurredAt\":\"2026-07-26T10:00:00Z\"," +
                    "\"matchStatus\":\"playing\"," +
                    "\"phase\":\"Set 1\"," +
                    "\"sideA\":{" +
                    "\"label\":\"Squadra A\"," +
                    "\"points\":\"15\"," +
                    "\"games\":0," +
                    "\"sets\":0" +
                    "}," +
                    "\"sideB\":{" +
                    "\"label\":\"Squadra B\"," +
                    "\"points\":\"0\"," +
                    "\"games\":0," +
                    "\"sets\":0" +
                    "}" +
                    "}",
            snapshot.toJson()
        )
    }

    @Test
    fun authResponseParsesSession() {
        val session =
            ArenaAuthSession.fromJson(
                body = "{" +
                        "\"access_token\":\"access\"," +
                        "\"refresh_token\":\"refresh\"," +
                        "\"expires_in\":3600" +
                        "}",
                receivedAtMillis = 123L
            )

        assertEquals("access", session.accessToken)
        assertEquals("refresh", session.refreshToken)
        assertEquals(3600L, session.expiresIn)
        assertEquals(123L, session.receivedAtMillis)
    }

    @Test
    fun apiResponseParsesConflict() {
        val result =
            ArenaApiResult.fromHttp(
                statusCode = 409,
                body = "{\"error\":\"conflict\"}"
            )

        assertEquals(409, result.statusCode)
        assertFalse(result.success)
        assertFalse(result.retryable)
        assertEquals("{\"error\":\"conflict\"}", result.body)
    }

    @Test
    fun stateUrlUsesConfiguredBaseUrlAndCourtId() {
        val client =
            ArenaApiClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = FakeTransport()
            )

        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts/court-1/state",
            client.stateUrl()
        )
    }

    @Test
    fun unauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            FakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = "ok"
                )
            )

        val tokenProvider =
            CountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )

        val client =
            ArenaApiClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.sendStateBlocking(
                sampleSnapshot()
            )

        assertEquals(200, result.statusCode)
        assertTrue(result.success)
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
    fun conflictDoesNotRefresh() {
        val transport =
            FakeTransport(
                ArenaHttpResponse(
                    statusCode = 409,
                    body = "conflict"
                )
            )

        val tokenProvider =
            CountingTokenProvider(
                accessToken = "access",
                refreshedToken = "fresh"
            )

        val client =
            ArenaApiClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.sendStateBlocking(
                sampleSnapshot()
            )

        assertEquals(409, result.statusCode)
        assertEquals(0, tokenProvider.refreshCount)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun liveMatchUrlUsesConfiguredBaseUrlAndCourtId() {
        val client =
            ArenaLiveMatchClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = FakeTransport()
            )

        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts/court-1/live-match",
            client.liveMatchUrl()
        )
    }

    @Test
    fun liveMatchParsesPayloadWithMatch() {
        val result =
            ArenaLiveMatchResult.fromHttp(
                statusCode = 200,
                body = liveMatchBody()
            )

        val response =
            requireNotNull(
                result.response
            )

        assertTrue(result.success)
        assertEquals("court-1", response.courtId)
        assertEquals("Campo Arena 1", response.arenaCourtLabel)
        assertEquals("Campo 1", response.tournamentCourtName)
        assertEquals("match-1", response.match?.matchId)
        assertEquals("Pippo / Pluto", response.match?.sideA?.label)
        assertEquals("Minny / Topolino", response.match?.sideB?.label)
        assertEquals("8-5", response.match?.score)
        assertEquals(8, response.match?.scoreA)
        assertEquals(5, response.match?.scoreB)
        assertEquals(0, response.match?.setsA)
        assertEquals(0, response.match?.setsB)
        assertEquals(1001, response.match?.lastLifecycleEventSequence)
        assertEquals("playing", response.match?.status)
        assertEquals("Set 1", response.match?.phase)
        assertEquals("game", response.match?.scoreMode)
    }

    @Test
    fun liveMatchParsesNullMatchWithReason() {
        val result =
            ArenaLiveMatchResult.fromHttp(
                statusCode = 200,
                body = "{" +
                        "\"ok\":true," +
                        "\"courtId\":\"court-1\"," +
                        "\"arenaCourtLabel\":\"Campo Arena 1\"," +
                        "\"tournamentCourtName\":null," +
                        "\"match\":null," +
                        "\"reason\":\"no_live_match\"" +
                        "}"
            )

        val response =
            requireNotNull(
                result.response
            )

        assertTrue(result.success)
        assertNull(response.match)
        assertEquals("no_live_match", response.reason)
    }

    @Test
    fun liveMatchParsesNullScoreAsZeroValues() {
        val result =
            ArenaLiveMatchResult.fromHttp(
                statusCode = 200,
                body = "{" +
                        "\"ok\":true," +
                        "\"courtId\":\"court-1\"," +
                        "\"arenaCourtLabel\":\"Campo Arena 1\"," +
                        "\"tournamentCourtName\":\"Campo 1\"," +
                        "\"match\":{" +
                        "\"matchId\":\"match-1\"," +
                        "\"sideA\":{\"label\":\"Squadra A\"}," +
                        "\"sideB\":{\"label\":\"Squadra B\"}," +
                        "\"score\":null," +
                        "\"status\":\"playing\"," +
                        "\"phase\":\"Set 1\"," +
                        "\"scoreMode\":\"game\"" +
                        "}," +
                        "\"reason\":null" +
                        "}"
            )

        assertTrue(result.success)
        assertNull(result.response?.match?.score)
        assertEquals(0, result.response?.match?.scoreA)
        assertEquals(0, result.response?.match?.scoreB)
        assertEquals(0, result.response?.match?.setsA)
        assertEquals(0, result.response?.match?.setsB)
    }

    @Test
    fun liveMatchUnauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            FakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = liveMatchBody()
                )
            )

        val tokenProvider =
            CountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )

        val client =
            ArenaLiveMatchClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.fetchLiveMatchBlocking()

        assertTrue(result.success)
        assertEquals(1, tokenProvider.refreshCount)
        assertEquals(2, transport.requests.size)
        assertEquals("GET", transport.requests[0].method)
        assertEquals("", transport.requests[0].body)
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
    fun liveMatchClientDoesNotLogTokensOrResponseBodies() {
        val source =
            java.io.File(
                "src/main/java/com/example/padelboardarena/arena/ArenaLiveMatchClient.kt"
            ).readText()

        assertFalse(source.contains("Log."))
        assertFalse(source.contains("println"))
        assertFalse(source.contains("printStackTrace"))
    }

    @Test
    fun courtsUrlUsesConfiguredBaseUrlOnly() {
        val client =
            ArenaCourtsClient(
                config = sampleConfig().copy(
                    courtId = ""
                ),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = FakeTransport()
            )

        assertEquals(
            "http://10.0.2.2:3000/api/arena/courts",
            client.courtsUrl()
        )
    }

    @Test
    fun courtsClientParsesActiveInactiveAndUnmappedCourts() {
        val result =
            ArenaCourtsResult.fromHttp(
                statusCode = 200,
                body = courtsBody()
            )

        assertTrue(result.success)
        assertEquals(3, result.courts.size)
        assertEquals("court-1", result.courts[0].courtId)
        assertEquals("center-1", result.courts[0].centerId)
        assertEquals("Centro Rossi", result.courts[0].centerName)
        assertEquals("Campo Arena 1", result.courts[0].label)
        assertEquals("Campo 1", result.courts[0].tournamentCourtName)
        assertTrue(result.courts[0].isActive)
        assertNull(result.courts[1].tournamentCourtName)
        assertFalse(result.courts[2].isActive)
    }

    @Test
    fun courtsClientUnauthorizedRefreshesOnceAndRetriesOnce() {
        val transport =
            FakeTransport(
                ArenaHttpResponse(
                    statusCode = 401,
                    body = "unauthorized"
                ),
                ArenaHttpResponse(
                    statusCode = 200,
                    body = courtsBody()
                )
            )

        val tokenProvider =
            CountingTokenProvider(
                accessToken = "expired",
                refreshedToken = "fresh"
            )

        val client =
            ArenaCourtsClient(
                config = sampleConfig(),
                tokenProvider = tokenProvider,
                transport = transport
            )

        val result =
            client.fetchCourtsBlocking()

        assertTrue(result.success)
        assertEquals(1, tokenProvider.refreshCount)
        assertEquals(2, transport.requests.size)
        assertEquals("GET", transport.requests[0].method)
        assertEquals("", transport.requests[0].body)
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
    fun courtsClientDoesNotLogTokensOrResponseBodies() {
        val source =
            java.io.File(
                "src/main/java/com/example/padelboardarena/arena/ArenaCourtsClient.kt"
            ).readText()

        assertFalse(source.contains("Log."))
        assertFalse(source.contains("println"))
        assertFalse(source.contains("printStackTrace"))
        assertFalse(source.contains("result.body"))
    }

    @Test
    fun courtsClientTimeoutReturnsControlledError() {
        val client =
            ArenaCourtsClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = ThrowingTransport()
            )

        val result =
            client.fetchCourtsBlocking()

        assertFalse(result.success)
        assertEquals(0, result.statusCode)
        assertEquals("network_error", result.body)
    }

    @Test
    fun courtsClientCanSucceedOnManualRetryAfterTimeout() {
        val failedClient =
            ArenaCourtsClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = ThrowingTransport()
            )
        val retryClient =
            ArenaCourtsClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = FakeTransport(
                    ArenaHttpResponse(
                        statusCode = 200,
                        body = courtsBody()
                    )
                )
            )

        assertFalse(
            failedClient.fetchCourtsBlocking().success
        )
        assertTrue(
            retryClient.fetchCourtsBlocking().success
        )
    }

    @Test
    fun liveMatchTimeoutReturnsControlledError() {
        val client =
            ArenaLiveMatchClient(
                config = sampleConfig(),
                tokenProvider = StaticTokenProvider(
                    accessToken = "access"
                ),
                transport = ThrowingTransport()
            )

        val result =
            client.fetchLiveMatchBlocking()

        assertFalse(result.success)
        assertEquals(0, result.statusCode)
        assertEquals("network_error", result.body)
        assertNull(result.response)
    }

    private fun sampleSnapshot(): ArenaScoreSnapshot {
        return ArenaScoreSnapshot(
            eventId = "uuid",
            eventSequence = 1,
            occurredAt = "2026-07-26T10:00:00Z",
            matchStatus = "playing",
            phase = "Set 1",
            sideA = ArenaScoreSide(
                label = "Squadra A",
                points = "15",
                games = 0,
                sets = 0
            ),
            sideB = ArenaScoreSide(
                label = "Squadra B",
                points = "0",
                games = 0,
                sets = 0
            )
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

    private fun liveMatchBody(): String {
        return "{" +
                "\"ok\":true," +
                "\"courtId\":\"court-1\"," +
                "\"arenaCourtLabel\":\"Campo Arena 1\"," +
                "\"tournamentCourtName\":\"Campo 1\"," +
                "\"match\":{" +
                "\"matchId\":\"match-1\"," +
                "\"sideA\":{" +
                "\"label\":\"Pippo / Pluto\"," +
                "\"teamId\":\"team-a\"" +
                "}," +
                "\"sideB\":{" +
                "\"label\":\"Minny / Topolino\"," +
                "\"teamId\":\"team-b\"" +
                "}," +
                "\"score\":\"8-5\"," +
                "\"scoreA\":8," +
                "\"scoreB\":5," +
                "\"setsA\":0," +
                "\"setsB\":0," +
                "\"lastLifecycleEventSequence\":1001," +
                "\"status\":\"playing\"," +
                "\"phase\":\"Set 1\"," +
                "\"scoreMode\":\"game\"" +
                "}," +
                "\"reason\":null" +
                "}"
    }

    private fun courtsBody(): String {
        return "{" +
                "\"ok\":true," +
                "\"courts\":[" +
                "{" +
                "\"courtId\":\"court-1\"," +
                "\"centerId\":\"center-1\"," +
                "\"centerName\":\"Centro Rossi\"," +
                "\"label\":\"Campo Arena 1\"," +
                "\"tournamentCourtName\":\"Campo 1\"," +
                "\"isActive\":true" +
                "}," +
                "{" +
                "\"courtId\":\"court-2\"," +
                "\"centerId\":\"center-1\"," +
                "\"centerName\":\"Centro Rossi\"," +
                "\"label\":\"Campo Arena 2\"," +
                "\"tournamentCourtName\":null," +
                "\"isActive\":true" +
                "}," +
                "{" +
                "\"courtId\":\"court-3\"," +
                "\"centerId\":\"center-2\"," +
                "\"centerName\":\"Centro Blu\"," +
                "\"label\":\"Campo Arena 3\"," +
                "\"tournamentCourtName\":\"Campo 3\"," +
                "\"isActive\":false" +
                "}" +
                "]" +
                "}"
    }
}

private class FakeTransport(
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
        requests.add(request)

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

private class StaticTokenProvider(
    private val accessToken: String
) : ArenaTokenProvider {
    override fun currentAccessToken(): String {
        return accessToken
    }

    override fun refreshAccessToken(): String? {
        return null
    }
}

private class CountingTokenProvider(
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

private class ThrowingTransport : ArenaHttpTransport {
    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        throw SocketTimeoutException(
            "failed to connect"
        )
    }
}
