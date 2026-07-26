package com.example.padelboardarena.arena

import java.time.Instant
import java.util.UUID

class ArenaManualSnapshotFactory(
    private val sequenceStore: ArenaManualSequenceStore,
    private val now: () -> Instant =
        { Instant.now() },
    private val newEventId: () -> String =
        { UUID.randomUUID().toString() }
) {
    fun createSnapshot(): ArenaScoreSnapshot {
        return ArenaScoreSnapshot(
            eventId = newEventId(),
            eventSequence = sequenceStore.nextSequence(),
            occurredAt = now().toString(),
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
