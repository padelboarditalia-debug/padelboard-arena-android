package com.example.padelboardarena

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityMatchLifecycleSourceTest {
    @Test
    fun connectedLongPressFinishesAndStandaloneLongPressDoesNotCallBackend() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun handleLongPress()",
                endMarker = "private fun assignDevice("
            )

        assertTrue(body.contains("if (standaloneClassicMode)"))
        assertTrue(body.contains("Nessuna azione configurata"))
        assertTrue(body.contains("finishCurrentMatch()"))
        assertTrue(
            body.indexOf("return") <
                    body.indexOf("finishCurrentMatch()")
        )
    }

    @Test
    fun doublePressUsesUndoUnlessMatchWasFinishedByArena() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun handleDoublePress()",
                endMarker = "private fun handleLongPress()"
            )

        assertTrue(body.contains("if (standaloneClassicMode)"))
        assertTrue(body.contains("MatchLifecycleState.ACTIVE"))
        assertTrue(body.contains("undoLastAction()"))
        assertTrue(body.contains("MatchLifecycleState.FINISHED_BY_ARENA"))
        assertTrue(body.contains("reopenFinishedMatch()"))
    }

    @Test
    fun finishCapturesSnapshotBeforeBackendCallAndResetsOnlyAfterSuccess() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )

        assertTrue(body.contains("val finishedSnapshot"))
        assertTrue(body.contains("captureCurrentScoreSnapshot()"))
        assertTrue(body.contains("lifecycleClient.finishMatch("))
        assertTrue(
            body.indexOf("captureCurrentScoreSnapshot()") <
                    body.indexOf("lifecycleClient.finishMatch(")
        )
        assertTrue(body.contains("isFinishSuccess(result.status)"))
        assertTrue(body.contains("resetLocalScoreSilently()"))
        assertTrue(
            body.indexOf("isFinishSuccess(result.status)") <
                    body.indexOf("resetLocalScoreSilently()")
        )
    }

    @Test
    fun finishStoresLastClosedMatchForLaterSettingsCorrection() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )

        assertTrue(
            body.contains(
                "lastClosedMatchStore.save("
            )
        )
        assertTrue(
            body.contains(
                "ArenaLastClosedMatch("
            )
        )
        assertTrue(
            body.contains(
                "matchId = resolvedMatchId"
            )
        )
        assertTrue(
            body.contains(
                "courtId = selection.selectedCourtId"
            )
        )
        assertTrue(
            body.contains(
                "teamLabelA = teamALabel"
            )
        )
        assertTrue(
            body.contains(
                "teamLabelB = teamBLabel"
            )
        )
        assertTrue(
            body.contains(
                "reopenAvailable = true"
            )
        )
    }

    @Test
    fun finishUsesDedicatedLifecycleSequenceStore() {
        val source =
            mainActivitySource()
        val finishBody =
            methodSlice(
                source = source,
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )
        val configureBody =
            methodSlice(
                source = source,
                startMarker = "private fun configureArenaCourtClients(",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertTrue(source.contains("private val arenaLifecycleSequenceStore by lazy"))
        assertTrue(finishBody.contains("arenaLifecycleSequenceStore.nextSequence()"))
        assertFalse(finishBody.contains("arenaSequenceStore.nextSequence()"))
        assertTrue(configureBody.contains("sequenceStore ="))
        assertTrue(configureBody.contains("arenaSequenceStore"))
    }

    @Test
    fun reopenUsesDedicatedLifecycleSequenceAndRestoresFinishedSnapshot() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun reopenFinishedMatch()",
                endMarker = "private fun reopenLastClosedMatchForCorrection()"
            )

        assertTrue(body.contains("arenaLifecycleSequenceStore.nextSequence()"))
        assertFalse(body.contains("arenaSequenceStore.nextSequence()"))
        assertTrue(body.contains("lifecycleClient.reopenMatch("))
        assertTrue(
            Regex(
                """restoreScoreSnapshot\s*\(\s*finishedSnapshot\s*\)"""
            ).containsMatchIn(body)
        )
        assertTrue(body.contains("scoreHistory.clear()"))
        assertTrue(body.contains("clearMatchLifecycleState()"))
    }

    @Test
    fun settingsRequestReopensLastClosedMatchThroughMain() {
        val source =
            mainActivitySource()
        val launcherBody =
            methodSlice(
                source = source,
                startMarker = "private val arenaSettingsLauncher =",
                endMarker = "private val permissionLauncher ="
            )

        assertTrue(
            launcherBody.contains(
                "ArenaSettingsActivity.REQUEST_REOPEN_LAST_FINISHED_MATCH"
            )
        )
        assertTrue(
            launcherBody.contains(
                "reopenLastClosedMatchForCorrection()"
            )
        )
    }

    @Test
    fun lastClosedReopenSuspendsCurrentMatchAndDoesNotPostState() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun reopenLastClosedMatchForCorrection()",
                endMarker = "private fun captureSuspendedCurrentMatch()"
            )

        assertTrue(
            body.contains(
                "lastClosedMatchStore.read()"
            )
        )
        assertTrue(
            body.contains(
                "captureSuspendedCurrentMatch()"
            )
        )
        assertTrue(
            body.contains(
                "stopLiveMatchPolling()"
            )
        )
        assertTrue(
            body.contains(
                "lifecycleClient.reopenMatch("
            )
        )
        assertTrue(
            body.contains(
                "restoreScoreSnapshot("
            )
        )
        assertTrue(
            body.contains(
                "lastClosedMatch.toScoreSnapshot()"
            )
        )
        assertTrue(
            body.contains(
                "lastClosedMatchStore.markReopenUnavailable()"
            )
        )
        assertTrue(
            body.contains(
                "restoreSuspendedCurrentMatch("
            )
        )
        assertFalse(
            body.contains(
                "enqueueArenaSnapshot("
            )
        )
        assertFalse(
            body.contains(
                "adoptBootstrappedLiveScoreIfNeeded("
            )
        )
    }

    @Test
    fun lastClosedReopenSuccessRestoresPreviousMatchAsActiveCorrection() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun reopenLastClosedMatchForCorrection()",
                endMarker = "private fun captureSuspendedCurrentMatch()"
            )

        assertTrue(body.contains("currentLiveMatchId ="))
        assertTrue(body.contains("lastClosedMatch.matchId"))
        assertTrue(body.contains("correctingPreviousMatchId ="))
        assertTrue(body.contains("restoreScoreSnapshot("))
        assertTrue(body.contains("lastClosedMatch.toScoreSnapshot()"))
        assertTrue(body.contains("clearMatchLifecycleState()"))
        assertTrue(body.contains("lastClosedMatchStore.markReopenUnavailable()"))
        assertTrue(body.contains("reopenedClosedMatchForCorrection = true"))
        assertTrue(body.contains("restartBleScanSafely("))
        assertTrue(body.contains("reopened_previous_match"))
    }

    @Test
    fun scoreSnapshotRestoreIncludesLocalFinishedAndFullScoreState() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun restoreScoreSnapshot(",
                endMarker = "private fun clearFinishedLifecycleIfCourtChanged("
            )

        assertTrue(body.contains("pointsA = snapshot.pointsA"))
        assertTrue(body.contains("pointsB = snapshot.pointsB"))
        assertTrue(body.contains("gamesA = snapshot.gamesA"))
        assertTrue(body.contains("gamesB = snapshot.gamesB"))
        assertTrue(body.contains("setsA = snapshot.setsA"))
        assertTrue(body.contains("setsB = snapshot.setsB"))
        assertTrue(body.contains("advantageSide = snapshot.advantageSide"))
        assertTrue(body.contains("killerMode = snapshot.killerMode"))
        assertTrue(body.contains("tieBreakActive = snapshot.tieBreakActive"))
        assertTrue(body.contains("tieBreakPointsA = snapshot.tieBreakPointsA"))
        assertTrue(body.contains("tieBreakPointsB = snapshot.tieBreakPointsB"))
        assertTrue(body.contains("localMatchFinished = snapshot.localMatchFinished"))
    }

    @Test
    fun correctionModeIgnoresSuspendedLiveMatchPollingResponses() {
        val source =
            mainActivitySource()
        val body =
            methodSlice(
                source = source,
                startMarker = "private fun applyLiveMatchResponse(",
                endMarker = "private fun activateNewLiveMatch("
            )

        assertTrue(source.contains("private var correctingPreviousMatchId: String? = null"))
        assertTrue(body.contains("val correctionMatchId ="))
        assertTrue(body.contains("if (correctionMatchId != null)"))
        assertTrue(body.contains("reason=no_live_match"))
        assertTrue(body.contains("match.matchId != correctionMatchId"))
        assertTrue(body.contains("reason=suspended_match"))
        assertTrue(body.contains("return"))
        assertTrue(body.contains("Arena correction polling accepted"))
    }

    @Test
    fun correctionModeKeepsInputDispatchActiveForPreviousMatch() {
        val source =
            mainActivitySource()
        val dispatchBody =
            methodSlice(
                source = source,
                startMarker = "private fun handleButtonEvent(",
                endMarker = "private fun correctGameForSide("
            )
        val registerBody =
            methodSlice(
                source = source,
                startMarker = "private fun registerPoint(",
                endMarker = "private fun announceValidPoint("
            )
        val finishBody =
            methodSlice(
                source = source,
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )

        assertTrue(dispatchBody.contains("Arena input dispatch:"))
        assertTrue(dispatchBody.contains("ButtonEvent.SINGLE_PRESS"))
        assertTrue(dispatchBody.contains("ButtonEvent.DOUBLE_PRESS"))
        assertTrue(dispatchBody.contains("ButtonEvent.TRIPLE_PRESS"))
        assertTrue(dispatchBody.contains("ButtonEvent.LONG_PRESS"))
        assertTrue(registerBody.contains("matchLifecycleState !="))
        assertTrue(registerBody.contains("correctionMatchId"))
        assertTrue(finishBody.contains("matchLifecycleState =="))
        assertTrue(finishBody.contains("MatchLifecycleState.REOPENING"))
    }

    @Test
    fun finishingCorrectionClearsCorrectionModeAndRestartsPolling() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )

        assertTrue(body.contains("if (reopenedClosedMatchForCorrection)"))
        assertTrue(body.contains("reopenedClosedMatchForCorrection = false"))
        assertTrue(body.contains("correctingPreviousMatchId = null"))
        assertTrue(body.contains("startLiveMatchPolling()"))
    }

    @Test
    fun failedLastClosedReopenRestoresSuspendedMatchAndClearsCorrectionMode() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun reopenLastClosedMatchForCorrection()",
                endMarker = "private fun captureSuspendedCurrentMatch()"
            )

        assertTrue(body.contains("restoreSuspendedCurrentMatch("))
        assertTrue(body.contains("correctingPreviousMatchId = null"))
        assertTrue(body.contains("Match corrente mantenuto"))
        assertTrue(body.contains("startLiveMatchPolling()"))
    }

    @Test
    fun suspendedCurrentMatchKeepsScoreLabelsLifecycleAndHistoryOnFailure() {
        val source =
            mainActivitySource()
        val captureBody =
            methodSlice(
                source = source,
                startMarker = "private fun captureSuspendedCurrentMatch()",
                endMarker = "private fun restoreSuspendedCurrentMatch("
            )
        val restoreBody =
            methodSlice(
                source = source,
                startMarker = "private fun restoreSuspendedCurrentMatch(",
                endMarker = "private fun ArenaLastClosedMatch.toScoreSnapshot()"
            )

        assertTrue(captureBody.contains("currentLiveMatchId = currentLiveMatchId"))
        assertTrue(captureBody.contains("teamLabelA = teamALabel"))
        assertTrue(captureBody.contains("teamLabelB = teamBLabel"))
        assertTrue(captureBody.contains("snapshot = captureCurrentScoreSnapshot()"))
        assertTrue(captureBody.contains("matchLifecycleState = matchLifecycleState"))
        assertTrue(captureBody.contains("scoreHistory = scoreHistory.toList()"))
        assertTrue(restoreBody.contains("currentLiveMatchId ="))
        assertTrue(restoreBody.contains("updateTeamLabels("))
        assertTrue(restoreBody.contains("restoreScoreSnapshot("))
        assertTrue(restoreBody.contains("matchLifecycleState ="))
        assertTrue(restoreBody.contains("scoreHistory.clear()"))
        assertTrue(restoreBody.contains("scoreHistory.addAll("))
    }

    @Test
    fun lastClosedStorePreservesSnapshotAndMarksReopenUnavailable() {
        val source =
            File(
                "src/main/java/com/example/padelboardarena/arena/ArenaLastClosedMatchStore.kt"
            ).readText()

        assertTrue(source.contains("data class ArenaLastClosedMatch("))
        assertTrue(source.contains("val matchId: String"))
        assertTrue(source.contains("val courtId: String"))
        assertTrue(source.contains("val teamLabelA: String"))
        assertTrue(source.contains("val teamLabelB: String"))
        assertTrue(source.contains("val gamesA: Int"))
        assertTrue(source.contains("val gamesB: Int"))
        assertTrue(source.contains("val finishEventId: String"))
        assertTrue(source.contains("val finishEventSequence: Int"))
        assertTrue(source.contains("val reopenAvailable: Boolean"))
        assertTrue(source.contains("fun markReopenUnavailable()"))
    }

    @Test
    fun liveMatchResponseAlignsLifecycleSequenceBeforeFinishOrReopen() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun applyLiveMatchResponse(",
                endMarker = "private fun updateTeamLabels("
            )

        assertTrue(body.contains("arenaLifecycleSequenceStore.advanceToAtLeast("))
        assertTrue(body.contains("match.lastLifecycleEventSequence"))
        assertTrue(
            body.indexOf("arenaLifecycleSequenceStore.advanceToAtLeast(") <
                    body.indexOf("currentLiveMatchId")
        )
    }

    @Test
    fun finishedLifecycleColdStartCanAdoptNextLiveMatchAfterRestore() {
        val source =
            mainActivitySource()
        val bootstrapBody =
            methodSlice(
                source = source,
                startMarker = "private fun bootstrapArenaSession()",
                endMarker = "private fun startLiveMatchPolling()"
            )
        val applyBody =
            methodSlice(
                source = source,
                startMarker = "private fun applyLiveMatchResponse(",
                endMarker = "private fun activateNewLiveMatch("
            )
        val activateBody =
            methodSlice(
                source = source,
                startMarker = "private fun activateNewLiveMatch(",
                endMarker = "private fun updateTeamLabels("
            )
        val emptyScoreBody =
            methodSlice(
                source = source,
                startMarker = "private fun isLocalScoreEmptyForLiveMatchBootstrap()",
                endMarker = "private fun bootstrapLocalScoreFromLiveMatch("
            )
        val bootstrapScoreBody =
            methodSlice(
                source = source,
                startMarker = "private fun bootstrapLocalScoreFromLiveMatch(",
                endMarker = "private fun adoptBootstrappedLiveScoreIfNeeded("
            )

        assertTrue(bootstrapBody.contains("ArenaSessionRestoreResult.RESTORED"))
        assertTrue(bootstrapBody.contains("activityVisible"))
        assertTrue(bootstrapBody.contains("startLiveMatchPolling()"))
        assertTrue(applyBody.contains("matchLifecycleState == MatchLifecycleState.FINISHED_BY_ARENA"))
        assertTrue(applyBody.contains("lastFinishedMatchId != match.matchId"))
        assertTrue(applyBody.contains("replacesFinishedMatch"))
        assertTrue(applyBody.contains("activateNewLiveMatch("))
        assertTrue(applyBody.contains("bootstrapLocalScoreFromLiveMatch("))
        assertTrue(applyBody.contains("adoptBootstrappedLiveScoreIfNeeded("))
        assertTrue(activateBody.contains("clearMatchLifecycleState()"))
        assertTrue(activateBody.contains("localMatchFinished = false"))
        assertTrue(emptyScoreBody.contains("gamesA == 0"))
        assertTrue(emptyScoreBody.contains("gamesB == 0"))
        assertTrue(emptyScoreBody.contains("matchLifecycleState == MatchLifecycleState.ACTIVE"))
        assertTrue(bootstrapScoreBody.contains("gamesA ="))
        assertTrue(bootstrapScoreBody.contains("match.scoreA.coerceAtLeast("))
        assertTrue(bootstrapScoreBody.contains("gamesB ="))
        assertTrue(bootstrapScoreBody.contains("match.scoreB.coerceAtLeast("))
    }

    @Test
    fun lifecycleDiagnosticsLogBackendResponseWithoutChangingUiMessages() {
        val source =
            mainActivitySource()
        val finishBody =
            methodSlice(
                source = source,
                startMarker = "private fun finishCurrentMatch()",
                endMarker = "private fun reopenFinishedMatch()"
            )
        val reopenBody =
            methodSlice(
                source = source,
                startMarker = "private fun reopenFinishedMatch()",
                endMarker = "private fun logArenaLifecycleResult("
            )
        val logBody =
            methodSlice(
                source = source,
                startMarker = "private fun logArenaLifecycleResult(",
                endMarker = "private fun resetLocalScoreSilently()"
            )

        assertTrue(finishBody.contains("logArenaLifecycleResult("))
        assertTrue(finishBody.contains("operation = \"finish\""))
        assertTrue(reopenBody.contains("logArenaLifecycleResult("))
        assertTrue(reopenBody.contains("operation = \"reopen\""))
        assertTrue(logBody.contains("result.statusCode"))
        assertTrue(logBody.contains("result.body"))
        assertTrue(logBody.contains("eventId"))
        assertTrue(logBody.contains("eventSequence"))
        assertTrue(logBody.contains("matchId"))
        assertTrue(logBody.contains("gamesA"))
        assertTrue(logBody.contains("gamesB"))
        assertTrue(logBody.contains("setsA"))
        assertTrue(logBody.contains("setsB"))
        assertTrue(logBody.contains("network_error"))
        assertFalse(logBody.contains("Authorization"))
        assertFalse(logBody.contains("accessToken"))
        assertFalse(logBody.contains("refreshToken"))
        assertFalse(logBody.contains("password"))
        assertTrue(finishBody.contains("\"Partita conclusa\""))
        assertTrue(finishBody.contains("\"Chiusura partita fallita\""))
        assertTrue(reopenBody.contains("\"Partita riaperta\""))
        assertTrue(reopenBody.contains("\"Riapertura partita fallita\""))
    }

    @Test
    fun lifecycleStatePersistsEnoughDataForReopenAfterRestart() {
        val source =
            mainActivitySource()
        val loadBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadMatchLifecycleState()",
                endMarker = "private fun saveMatchLifecycleState()"
            )
        assertTrue(source.contains("PREF_LAST_FINISHED_COURT_ID"))
        assertTrue(source.contains("PREF_LAST_FINISHED_MATCH_ID"))
        assertTrue(source.contains("PREF_LAST_FINISH_EVENT_ID"))
        assertTrue(source.contains("PREF_LAST_FINISH_EVENT_SEQUENCE"))
        assertTrue(source.contains("PREF_LAST_FINISHED_GAMES_A"))
        assertTrue(source.contains("PREF_LAST_FINISHED_GAMES_B"))
        assertTrue(source.contains("PREF_LAST_FINISHED_SETS_A"))
        assertTrue(source.contains("PREF_LAST_FINISHED_SETS_B"))
        assertTrue(loadBody.contains("lastFinishedSnapshot ="))
        assertTrue(source.contains("private fun saveLastFinishedSnapshot()"))
        assertTrue(source.contains("val snapshot ="))
    }

    @Test
    fun resetLocalScoreSilentlyDoesNotPostArenaSnapshotOrTouchSync() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun resetLocalScoreSilently()",
                endMarker = "private fun captureCurrentScoreSnapshot()"
            )

        assertTrue(body.contains("pointsA = 0"))
        assertTrue(body.contains("gamesA = 0"))
        assertTrue(body.contains("setsA = 0"))
        assertTrue(body.contains("scoreHistory.clear()"))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("arenaRealScoreSync"))
        assertFalse(body.contains("ArenaRealScoreSync"))
    }

    @Test
    fun connectedPointEventsAreIgnoredWhileLifecycleIsNotActive() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun registerPoint(",
                endMarker = "private fun announceValidPoint("
            )

        assertTrue(
            Regex(
                """matchLifecycleState\s*!=\s*MatchLifecycleState\.ACTIVE"""
            ).containsMatchIn(body)
        )
        assertTrue(body.contains("Operazione in corso"))
        val lifecycleGuardIndex =
            Regex(
                """matchLifecycleState\s*!=\s*MatchLifecycleState\.ACTIVE"""
            ).find(body)?.range?.first ?: -1

        assertTrue(
            lifecycleGuardIndex >= 0
        )
        assertTrue(
            lifecycleGuardIndex <
                    body.indexOf("saveSnapshot(")
        )
    }

    @Test
    fun lifecycleDoesNotModifyBleParsingOrMatchingContracts() {
        val source =
            mainActivitySource()
        val parseBody =
            methodSlice(
                source = source,
            startMarker = "private fun parseShellyPacket(",
            endMarker = "private fun handleButtonEvent("
            )
        val assignmentBody =
            methodSlice(
                source = source,
                startMarker = "private fun beginAssignment(",
                endMarker = "private fun parseShellyPacket("
            )

        assertFalse(parseBody.contains("MatchLifecycle"))
        assertFalse(parseBody.contains("finishCurrentMatch"))
        assertFalse(parseBody.contains("reopenFinishedMatch"))
        assertFalse(assignmentBody.contains("MatchLifecycle"))
    }

    @Test
    fun finishReopenClientIsNotInjectedIntoArenaRealScoreSync() {
        val source =
            File(
                "src/main/java/com/example/padelboardarena/arena/ArenaRealScoreSync.kt"
            ).readText()

        assertFalse(source.contains("ArenaMatchLifecycleClient"))
        assertFalse(source.contains("finish-match"))
        assertFalse(source.contains("reopen-match"))
    }

    @Test
    fun lifecycleEnumContainsRequestedStates() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "enum class MatchLifecycleState",
                endMarker = "data class ParsedShellyPacket("
            )

        assertTrue(body.contains("ACTIVE"))
        assertTrue(body.contains("FINISHING"))
        assertTrue(body.contains("FINISHED_BY_ARENA"))
        assertTrue(body.contains("REOPENING"))
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

        assertTrue(
            start >= 0
        )
        assertTrue(
            end > start
        )

        return source.substring(
            start,
            end
        )
    }
}
