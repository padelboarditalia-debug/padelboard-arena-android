package com.example.padelboardarena.arena

import java.time.Instant
import java.util.UUID

class ArenaManualTestSender(
    private val config: ArenaConfig =
        ArenaBuildConfig.load(),
    private val authClient: ArenaAuthClient =
        ArenaAuthClient(config),
    private val apiClientFactory:
        (ArenaTokenProvider) -> ArenaApiClient =
        { tokenProvider ->
            ArenaApiClient(
                config = config,
                tokenProvider = tokenProvider
            )
        }
) {
    fun sendSampleState(
        password: String,
        callback: (ArenaApiResult) -> Unit
    ) {
        Thread {
            authClient.login(
                password = password
            )

            val apiClient =
                apiClientFactory(authClient)

            apiClient.sendState(
                snapshot = sampleSnapshot(),
                callback = callback
            )
        }.start()
    }

    fun sampleSnapshot(): ArenaScoreSnapshot {
        return ArenaScoreSnapshot(
            eventId = UUID.randomUUID().toString(),
            eventSequence = 1,
            occurredAt = Instant.now().toString(),
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
}
