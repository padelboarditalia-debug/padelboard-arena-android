package com.example.padelboardarena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArenaScoreAnnouncerSourceTest {
    @Test
    fun closeEventsUseQueueFlush() {
        val source =
            announcerSource()

        assertTrue(
            source.contains(
                "TextToSpeech.QUEUE_FLUSH"
            )
        )
    }

    @Test
    fun onDestroyShutsDownTextToSpeech() {
        val source =
            mainActivitySource()
        val onDestroyBody =
            methodSlice(
                source = source,
                startMarker = "override fun onDestroy()",
                endMarker = "super.onDestroy()"
            )

        assertTrue(
            onDestroyBody.contains(
                "scoreAnnouncer.shutdown()"
            )
        )
    }

    @Test
    fun invalidOrEmptyUndoDoesNotAnnounce() {
        val undoBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun undoLastAction()",
                endMarker = "private fun resetMatch()"
            )

        val emptyReturnIndex =
            undoBody.indexOf(
                "return"
            )
        val announceIndex =
            undoBody.indexOf(
                "announceValidUndo("
            )

        assertTrue(emptyReturnIndex >= 0)
        assertTrue(announceIndex > emptyReturnIndex)
        assertEquals(
            1,
            Regex(
                """announceValidUndo\s*\("""
            ).findAll(undoBody).count()
        )
    }

    @Test
    fun deduplicatedBleEventsReturnBeforeScoringAndSpeech() {
        val processBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun processScanResult(",
                endMarker = "private fun extractShellyMac("
            )

        val duplicateCheckIndex =
            processBody.indexOf(
                "previousPacketId == packetId"
            )
        val duplicateReturnIndex =
            processBody.indexOf(
                "return",
                duplicateCheckIndex
            )
        val handleEventIndex =
            processBody.lastIndexOf(
                "handleButtonEvent("
            )

        assertTrue(duplicateCheckIndex >= 0)
        assertTrue(duplicateReturnIndex > duplicateCheckIndex)
        assertTrue(handleEventIndex > duplicateReturnIndex)
        assertFalse(
            processBody.contains(
                "announceValidPoint("
            )
        )
        assertFalse(
            processBody.contains(
                "announceValidUndo("
            )
        )
    }

    @Test
    fun pointSpeechHappensOnlyAfterStateUpdateAndAnimation() {
        val registerBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun registerPoint(",
                endMarker = "private fun announceValidPoint("
            )

        val updateCount =
            Regex(
                """saveStateUpdateScreenAndEnqueueArenaSnapshot\s*\("""
            ).findAll(registerBody).count()
        val speechCount =
            Regex(
                """announceValidPoint\s*\("""
            ).findAll(registerBody).count()

        assertTrue(updateCount > 0)
        assertEquals(updateCount, speechCount)
        assertTrue(
            registerBody.indexOf(
                "saveStateUpdateScreenAndEnqueueArenaSnapshot()"
            ) < registerBody.indexOf(
                "announceValidPoint("
            )
        )
    }

    @Test
    fun speechFormatterDoesNotModifyScoringOrArenaSync() {
        val source =
            formatterSource()

        assertFalse(source.contains("registerPoint("))
        assertFalse(source.contains("undoLastAction("))
        assertFalse(Regex("""pointsA\s*=(?!=)""").containsMatchIn(source))
        assertFalse(Regex("""pointsB\s*=(?!=)""").containsMatchIn(source))
        assertFalse(Regex("""gamesA\s*=(?!=)""").containsMatchIn(source))
        assertFalse(Regex("""gamesB\s*=(?!=)""").containsMatchIn(source))
        assertFalse(source.contains("saveState()"))
        assertFalse(source.contains("ArenaRealScoreSync"))
        assertFalse(source.contains("enqueueArenaSnapshot()"))
    }

    private fun announcerSource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/ArenaScoreAnnouncer.kt"
        ).readText()
    }

    private fun formatterSource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/ArenaScoreSpeechFormatter.kt"
        ).readText()
    }

    private fun mainActivitySource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/MainActivity.kt"
        ).readText()
    }

    private fun methodSlice(
        source: String,
        startMarker: String,
        endMarker: String
    ): String {
        val start =
            source.indexOf(
                startMarker
            )
        val end =
            source.indexOf(
                endMarker,
                start
            )

        assertTrue(start >= 0)
        assertTrue(end > start)

        return source.substring(
            start,
            end
        )
    }
}
