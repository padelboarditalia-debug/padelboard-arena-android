package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ArenaManualFeatureTest {
    @Test
    fun manualSequencePersistsAcrossStoreInstances() {
        var savedSequence = 7

        val firstStore =
            PersistedArenaManualSequenceStore(
                readSequence = { savedSequence },
                writeSequence = { sequence ->
                    savedSequence = sequence
                }
            )

        assertEquals(8, firstStore.nextSequence())

        val secondStore =
            PersistedArenaManualSequenceStore(
                readSequence = { savedSequence },
                writeSequence = { sequence ->
                    savedSequence = sequence
                }
            )

        assertEquals(9, secondStore.nextSequence())
    }

    @Test
    fun manualSnapshotIncrementsSequenceOnce() {
        var sequence = 0
        var writeCount = 0

        val factory =
            ArenaManualSnapshotFactory(
                sequenceStore =
                    PersistedArenaManualSequenceStore(
                        readSequence = { sequence },
                        writeSequence = { next ->
                            writeCount += 1
                            sequence = next
                        }
                    ),
                now = {
                    Instant.parse(
                        "2026-07-26T10:00:00Z"
                    )
                },
                newEventId = {
                    "event-1"
                }
            )

        val snapshot =
            factory.createSnapshot()

        assertEquals(1, snapshot.eventSequence)
        assertEquals(1, writeCount)
        assertEquals(1, sequence)
    }

    @Test
    fun manualSnapshotMatchesArenaPayload() {
        val factory =
            ArenaManualSnapshotFactory(
                sequenceStore =
                    PersistedArenaManualSequenceStore(
                        readSequence = { 0 },
                        writeSequence = {}
                    ),
                now = {
                    Instant.parse(
                        "2026-07-26T10:00:00Z"
                    )
                },
                newEventId = {
                    "event-1"
                }
            )

        val snapshot =
            factory.createSnapshot()

        assertEquals("event-1", snapshot.eventId)
        assertEquals(1, snapshot.eventSequence)
        assertEquals(
            "2026-07-26T10:00:00Z",
            snapshot.occurredAt
        )
        assertEquals("playing", snapshot.matchStatus)
        assertEquals("Set 1", snapshot.phase)
        assertEquals("Squadra A", snapshot.sideA.label)
        assertEquals("15", snapshot.sideA.points)
        assertEquals(0, snapshot.sideA.games)
        assertEquals(0, snapshot.sideA.sets)
        assertEquals("Squadra B", snapshot.sideB.label)
        assertEquals("0", snapshot.sideB.points)
        assertEquals(0, snapshot.sideB.games)
        assertEquals(0, snapshot.sideB.sets)
    }

    @Test
    fun uiMessagesMapExpectedApiOutcomes() {
        assertEquals(
            "Invio Arena riuscito",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(200, "ok")
            )
        )
        assertEquals(
            "Invio Arena non autorizzato: 401",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(401, "")
            )
        )
        assertEquals(
            "Invio Arena vietato: 403",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(403, "")
            )
        )
        assertEquals(
            "Campo Arena non trovato: 404",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(404, "")
            )
        )
        assertEquals(
            "Invio Arena in conflitto: 409",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(409, "")
            )
        )
        assertEquals(
            "Errore server Arena: 500",
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(500, "")
            )
        )
    }

    @Test
    fun failedLoginMapsToInvalidCredentials() {
        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport =
                    ManualFakeTransport(
                        ArenaHttpResponse(
                            statusCode = 400,
                            body = "{\"error\":\"invalid_grant\"}"
                        )
                    )
            )

        val error =
            runCatching {
                authClient.login(
                    password = "wrong"
                )
            }.exceptionOrNull()

        assertTrue(error != null)
        assertEquals(
            "Login Arena non riuscito: credenziali non valide",
            ArenaManualUiMessages.loginFailed(
                error!!
            )
        )
    }

    @Test
    fun sendConflictReturns409() {
        val transport =
            ManualFakeTransport(
                ArenaHttpResponse(
                    statusCode = 409,
                    body = "conflict"
                )
            )

        val client =
            ArenaApiClient(
                config = sampleConfig(),
                tokenProvider =
                    ManualStaticTokenProvider(
                        accessToken = "access"
                    ),
                transport = transport
            )

        val result =
            client.sendStateBlocking(
                ArenaManualSnapshotFactory(
                    sequenceStore =
                        PersistedArenaManualSequenceStore(
                            readSequence = { 0 },
                            writeSequence = {}
                        ),
                    now = {
                        Instant.parse(
                            "2026-07-26T10:00:00Z"
                        )
                    },
                    newEventId = {
                        "event-1"
                    }
                ).createSnapshot()
            )

        assertEquals(409, result.statusCode)
        assertEquals(
            "Invio Arena in conflitto: 409",
            ArenaManualUiMessages.apiResult(result)
        )
    }

    private fun sampleConfig(): ArenaConfig {
        return ArenaConfig(
            apiBaseUrl = "http://10.0.2.2:3000",
            courtId = "court-1",
            supabaseUrl = "https://example.supabase.co",
            supabasePublishableKey = "publishable",
            email = "arena@example.com"
        )
    }
}

private class ManualFakeTransport(
    vararg responses: ArenaHttpResponse
) : ArenaHttpTransport {
    private val pendingResponses =
        ArrayDeque(
            responses.toList()
        )

    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        return pendingResponses.removeFirst()
    }
}

private class ManualStaticTokenProvider(
    private val accessToken: String
) : ArenaTokenProvider {
    override fun currentAccessToken(): String {
        return accessToken
    }

    override fun refreshAccessToken(): String? {
        return null
    }
}
