package com.example.padelboardarena.arena

import java.time.Instant
import java.util.UUID

data class ArenaRealScoreState(
    val pointsA: String,
    val pointsB: String,
    val gamesA: Int,
    val gamesB: Int
)

class ArenaRealScoreSnapshotFactory(
    private val sequenceStore: ArenaManualSequenceStore,
    private val now: () -> Instant =
        { Instant.now() },
    private val newEventId: () -> String =
        { UUID.randomUUID().toString() }
) {
    fun createSnapshot(
        state: ArenaRealScoreState
    ): ArenaScoreSnapshot {
        return ArenaScoreSnapshot(
            eventId = newEventId(),
            eventSequence = sequenceStore.nextSequence(),
            occurredAt = now().toString(),
            matchStatus = "playing",
            phase = "Set 1",
            sideA = ArenaScoreSide(
                label = "Squadra A",
                points = state.pointsA,
                games = state.gamesA,
                sets = 0
            ),
            sideB = ArenaScoreSide(
                label = "Squadra B",
                points = state.pointsB,
                games = state.gamesB,
                sets = 0
            )
        )
    }
}

interface ArenaScoreSnapshotSender {
    fun sendSnapshot(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaApiResult) -> Unit
    )
}

class ArenaApiScoreSnapshotSender(
    private val apiClient: ArenaApiClient
) : ArenaScoreSnapshotSender {
    override fun sendSnapshot(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaApiResult) -> Unit
    ) {
        apiClient.sendState(
            snapshot = snapshot,
            callback = callback
        )
    }
}

class ArenaRealScoreSync(
    private val snapshotFactory: ArenaRealScoreSnapshotFactory,
    private val sender: ArenaScoreSnapshotSender,
    private val onResult: (ArenaApiResult) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    fun enqueue(
        state: ArenaRealScoreState
    ) {
        try {
            val snapshot =
                snapshotFactory.createSnapshot(
                    state
                )

            sender.sendSnapshot(
                snapshot = snapshot,
                callback = onResult
            )
        } catch (error: Exception) {
            onError(error)
        }
    }
}
