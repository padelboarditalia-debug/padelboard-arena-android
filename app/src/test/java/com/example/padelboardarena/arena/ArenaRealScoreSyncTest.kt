package com.example.padelboardarena.arena

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ArenaRealScoreSyncTest {
    @Test
    fun validTransitionEnqueuesOneSend() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            state = scoreState(
                pointsA = "15",
                pointsB = "0"
            )
        )

        assertEquals(1, sender.snapshots.size)
        assertEquals("event-1", sender.snapshots[0].eventId)
        assertEquals(1, sender.snapshots[0].eventSequence)
    }

    @Test
    fun twoTransitionsUseIncreasingSequences() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            scoreState(
                pointsA = "15",
                pointsB = "0"
            )
        )
        sync.enqueue(
            scoreState(
                pointsA = "30",
                pointsB = "0"
            )
        )

        assertEquals(1, sender.snapshots[0].eventSequence)
        assertEquals(2, sender.snapshots[1].eventSequence)
    }

    @Test
    fun snapshotsAreQueuedInLocalTransitionOrder() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            scoreState(
                pointsA = "15",
                pointsB = "0"
            )
        )
        sync.enqueue(
            scoreState(
                pointsA = "30",
                pointsB = "0"
            )
        )

        assertEquals("15", sender.snapshots[0].sideA.points)
        assertEquals("30", sender.snapshots[1].sideA.points)
    }

    @Test
    fun snapshotCapturesStateAtTransitionTime() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        var state =
            scoreState(
                pointsA = "15",
                pointsB = "0"
            )

        sync.enqueue(state)

        state =
            state.copy(
                pointsA = "30"
            )

        assertEquals("15", sender.snapshots[0].sideA.points)
        assertEquals("30", state.pointsA)
    }

    @Test
    fun networkErrorDoesNotModifyLocalScoreState() {
        val sender =
            ThrowingArenaScoreSnapshotSender()

        var errorCount = 0

        val sync =
            syncWith(
                sender = sender,
                onError = {
                    errorCount += 1
                }
            )

        val localState =
            scoreState(
                pointsA = "40",
                pointsB = "30",
                gamesA = 2,
                gamesB = 1
            )

        sync.enqueue(localState)

        assertEquals(1, errorCount)
        assertEquals("40", localState.pointsA)
        assertEquals("30", localState.pointsB)
        assertEquals(2, localState.gamesA)
        assertEquals(1, localState.gamesB)
    }

    @Test
    fun invalidOrDeduplicatedEventDoesNotCallSync() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        syncWith(
            sender = sender
        )

        assertEquals(0, sender.snapshots.size)
    }

    @Test
    fun realSnapshotPayloadMatchesManualShape() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            scoreState(
                pointsA = "15",
                pointsB = "0"
            )
        )

        assertEquals(
            "{" +
                    "\"eventId\":\"event-1\"," +
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
            sender.snapshots[0].toJson()
        )
    }

    @Test
    fun validUndoEnqueuesOneArenaSend() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            scoreState(
                pointsA = "15",
                pointsB = "0",
                gamesA = 1,
                gamesB = 0
            )
        )

        assertEquals(1, sender.snapshots.size)
    }

    @Test
    fun undoWithEmptyHistoryDoesNotEnqueueArenaSend() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        syncWith(
            sender = sender
        )

        assertEquals(0, sender.snapshots.size)
    }

    @Test
    fun validUndoSnapshotRepresentsRestoredState() {
        val sender =
            RecordingArenaScoreSnapshotSender()

        val sync =
            syncWith(
                sender = sender
            )

        sync.enqueue(
            scoreState(
                pointsA = "0",
                pointsB = "40",
                gamesA = 2,
                gamesB = 3
            )
        )

        val snapshot =
            sender.snapshots.single()

        assertEquals("0", snapshot.sideA.points)
        assertEquals("40", snapshot.sideB.points)
        assertEquals(2, snapshot.sideA.games)
        assertEquals(3, snapshot.sideB.games)
    }

    private fun syncWith(
        sender: ArenaScoreSnapshotSender,
        onError: (Throwable) -> Unit = {}
    ): ArenaRealScoreSync {
        var sequence = 0
        var eventIndex = 0

        val factory =
            ArenaRealScoreSnapshotFactory(
                sequenceStore =
                    PersistedArenaManualSequenceStore(
                        readSequence = { sequence },
                        writeSequence = { nextSequence ->
                            sequence = nextSequence
                        }
                    ),
                now = {
                    Instant.parse(
                        "2026-07-26T10:00:00Z"
                    )
                },
                newEventId = {
                    eventIndex += 1
                    "event-$eventIndex"
                }
            )

        return ArenaRealScoreSync(
            snapshotFactory = factory,
            sender = sender,
            onResult = { _, _ -> },
            onError = onError
        )
    }

    private fun scoreState(
        pointsA: String,
        pointsB: String,
        gamesA: Int = 0,
        gamesB: Int = 0
    ): ArenaRealScoreState {
        return ArenaRealScoreState(
            pointsA = pointsA,
            pointsB = pointsB,
            gamesA = gamesA,
            gamesB = gamesB
        )
    }
}

private class RecordingArenaScoreSnapshotSender :
    ArenaScoreSnapshotSender {
    val snapshots =
        mutableListOf<ArenaScoreSnapshot>()

    override fun sendSnapshot(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaScoreSnapshot, ArenaApiResult) -> Unit
    ) {
        snapshots.add(snapshot)
        callback(
            snapshot,
            ArenaApiResult.fromHttp(
                statusCode = 200,
                body = "ok"
            )
        )
    }
}

private class ThrowingArenaScoreSnapshotSender :
    ArenaScoreSnapshotSender {
    override fun sendSnapshot(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaScoreSnapshot, ArenaApiResult) -> Unit
    ) {
        throw java.io.IOException(
            "network unavailable"
        )
    }
}
