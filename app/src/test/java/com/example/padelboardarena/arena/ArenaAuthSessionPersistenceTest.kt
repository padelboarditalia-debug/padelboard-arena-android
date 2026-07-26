package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaAuthSessionPersistenceTest {
    @Test
    fun successfulLoginStoresPersistentRefreshToken() {
        val store =
            RecordingArenaSessionStore()

        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport =
                    RecordingTransport(
                        sessionResponse(
                            accessToken = "access-login",
                            refreshToken = "refresh-login"
                        )
                    ),
                sessionStore = store
            )

        authClient.login(
            password = "manual-password"
        )

        assertEquals(
            "access-login",
            authClient.currentAccessToken()
        )
        assertEquals(
            "refresh-login",
            store.refreshToken
        )
        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun passwordIsNotStoredWithSession() {
        val store =
            RecordingArenaSessionStore()

        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport =
                    RecordingTransport(
                        sessionResponse(
                            accessToken = "access-login",
                            refreshToken = "refresh-login"
                        )
                    ),
                sessionStore = store
            )

        authClient.login(
            password = "secret-password"
        )

        assertFalse(
            store.storedValues.any { value ->
                value.contains(
                    "secret-password"
                )
            }
        )
    }

    @Test
    fun startupWithValidRefreshTokenRestoresSession() {
        val store =
            RecordingArenaSessionStore(
                refreshToken = "refresh-existing"
            )
        val transport =
            RecordingTransport(
                sessionResponse(
                    accessToken = "access-restored",
                    refreshToken = "refresh-rotated"
                )
            )
        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport = transport,
                sessionStore = store
            )

        val result =
            authClient.restorePersistedSession()

        assertEquals(
            ArenaSessionRestoreResult.RESTORED,
            result
        )
        assertEquals(
            "access-restored",
            authClient.currentAccessToken()
        )
        assertEquals(
            "refresh-rotated",
            store.refreshToken
        )
        assertEquals(
            "{\"refresh_token\":\"refresh-existing\"}",
            transport.requests[0].body
        )
    }

    @Test
    fun failedRefreshInvalidatesPersistentSession() {
        val store =
            RecordingArenaSessionStore(
                refreshToken = "refresh-invalid"
            )
        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport =
                    RecordingTransport(
                        ArenaHttpResponse(
                            statusCode = 400,
                            body = "invalid refresh"
                        )
                    ),
                sessionStore = store
            )

        val result =
            authClient.restorePersistedSession()

        assertEquals(
            ArenaSessionRestoreResult.FAILED,
            result
        )
        assertNull(
            authClient.currentAccessToken()
        )
        assertNull(
            store.refreshToken
        )
        assertEquals(
            1,
            store.clearCount
        )
    }

    @Test
    fun missingPersistentSessionKeepsManualLoginAvailable() {
        val store =
            RecordingArenaSessionStore()
        val transport =
            RecordingTransport()
        val authClient =
            ArenaAuthClient(
                config = sampleConfig(),
                transport = transport,
                sessionStore = store
            )

        val result =
            authClient.restorePersistedSession()

        assertEquals(
            ArenaSessionRestoreResult.MISSING,
            result
        )
        assertNull(
            authClient.currentAccessToken()
        )
        assertEquals(
            0,
            transport.requests.size
        )
        assertEquals(
            0,
            store.clearCount
        )
    }

    @Test
    fun uiMessagesDoNotIncludeTokens() {
        val message =
            ArenaManualUiMessages.apiResult(
                ArenaApiResult.fromHttp(
                    statusCode = 200,
                    body = "{\"access_token\":\"access-secret\",\"refresh_token\":\"refresh-secret\"}"
                )
            )

        assertEquals(
            "Arena aggiornata",
            message
        )
        assertFalse(
            message.contains(
                "access-secret"
            )
        )
        assertFalse(
            message.contains(
                "refresh-secret"
            )
        )
    }

    @Test
    fun authErrorDoesNotModifyLocalScoreState() {
        val sender =
            AuthErrorArenaScoreSnapshotSender()
        val localState =
            ArenaRealScoreState(
                pointsA = "40",
                pointsB = "30",
                gamesA = 2,
                gamesB = 1
            )
        var resultStatus = 0

        val sync =
            ArenaRealScoreSync(
                snapshotFactory =
                    ArenaRealScoreSnapshotFactory(
                        sequenceStore =
                            PersistedArenaManualSequenceStore(
                                readSequence = { 0 },
                                writeSequence = {}
                            ),
                        newEventId = { "event-auth-error" }
                    ),
                sender = sender,
                onResult = { _, result ->
                    resultStatus = result.statusCode
                },
                onError = {}
            )

        sync.enqueue(
            localState
        )

        assertEquals(
            401,
            resultStatus
        )
        assertEquals(
            "40",
            localState.pointsA
        )
        assertEquals(
            "30",
            localState.pointsB
        )
        assertEquals(
            2,
            localState.gamesA
        )
        assertEquals(
            1,
            localState.gamesB
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

    private fun sessionResponse(
        accessToken: String,
        refreshToken: String
    ): ArenaHttpResponse {
        return ArenaHttpResponse(
            statusCode = 200,
            body = "{" +
                    "\"access_token\":\"$accessToken\"," +
                    "\"refresh_token\":\"$refreshToken\"," +
                    "\"expires_in\":3600" +
                    "}"
        )
    }
}

private class RecordingArenaSessionStore(
    var refreshToken: String? = null
) : ArenaSessionStore {
    val storedValues =
        mutableListOf<String>()
    var saveCount = 0
    var clearCount = 0

    override fun readRefreshToken(): String? {
        return refreshToken
    }

    override fun saveSession(
        session: ArenaAuthSession
    ) {
        saveCount += 1
        refreshToken = session.refreshToken
        storedValues.add(
            session.refreshToken
        )
    }

    override fun clear() {
        clearCount += 1
        refreshToken = null
        storedValues.clear()
    }
}

private class RecordingTransport(
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

private class AuthErrorArenaScoreSnapshotSender : ArenaScoreSnapshotSender {
    override fun sendSnapshot(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaScoreSnapshot, ArenaApiResult) -> Unit
    ) {
        callback(
            snapshot,
            ArenaApiResult.fromHttp(
                statusCode = 401,
                body = "Missing access token"
            )
        )
    }
}