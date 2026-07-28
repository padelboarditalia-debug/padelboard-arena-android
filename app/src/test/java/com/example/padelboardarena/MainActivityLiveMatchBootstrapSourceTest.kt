package com.example.padelboardarena

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityLiveMatchBootstrapSourceTest {
    @Test
    fun settingsResultReadsRequestBeforeReloadAndResetsBeforeBootstrap() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private val arenaSettingsLauncher =",
                endMarker = "private val permissionLauncher ="
            )

        assertTrue(body.contains("val settingsRequest"))
        assertTrue(
            body.indexOf("val settingsRequest") <
                    body.indexOf("reloadArenaOperationMode()")
        )
        assertTrue(body.contains("ArenaSettingsActivity.REQUEST_RESET_SCORE"))
        assertTrue(body.contains("resetLocalScoreWithoutArenaSync("))
        assertTrue(
            body.indexOf("resetLocalScoreWithoutArenaSync(") <
                    body.indexOf("reloadArenaCourtSelection()")
        )
        assertTrue(
            body.indexOf("resetLocalScoreWithoutArenaSync(") <
                    body.indexOf("bootstrapArenaSession()")
        )
        assertFalse(body.contains("Reset locale non disponibile"))
    }

    @Test
    fun connectedResetDoesNotPostStateIncrementSequenceOrCallLifecycle() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private val arenaSettingsLauncher =",
                endMarker = "private val permissionLauncher ="
            )
        val resetBranch =
            body.substring(
                body.indexOf("ArenaSettingsActivity.REQUEST_RESET_SCORE"),
                body.indexOf("else -> {")
            )

        assertTrue(resetBranch.contains("resetLocalScoreWithoutArenaSync("))
        assertFalse(resetBranch.contains("enqueueArenaSnapshot("))
        assertFalse(resetBranch.contains("ArenaRealScoreSync"))
        assertFalse(resetBranch.contains("arenaSequenceStore.nextSequence()"))
        assertFalse(resetBranch.contains("finishCurrentMatch("))
        assertFalse(resetBranch.contains("reopenFinishedMatch("))
        assertFalse(resetBranch.contains("finishMatch("))
        assertFalse(resetBranch.contains("reopenMatch("))
    }

    @Test
    fun resetHelperDoesNotPostStateIncrementSequenceAnimateOrSpeak() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun resetLocalScoreWithoutArenaSync(",
                endMarker = "private fun captureCurrentScoreSnapshot()"
            )

        assertTrue(body.contains("resetLocalScoreSilently()"))
        assertTrue(body.contains("saveState()"))
        assertTrue(body.contains("updateScreen()"))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("arenaRealScoreSync"))
        assertFalse(body.contains("arenaSequenceStore.nextSequence()"))
        assertFalse(body.contains("finishMatch("))
        assertFalse(body.contains("reopenMatch("))
        assertFalse(body.contains("flashScore"))
        assertFalse(body.contains("announce"))
    }

    @Test
    fun liveMatchBootstrapUsesOfficialScoreOnlyWhenAllowed() {
        val body =
            applyLiveMatchResponseBody()

        assertTrue(body.contains("previousLiveMatchId"))
        assertTrue(body.contains("matchChanged"))
        assertTrue(body.contains("replacesFinishedMatch"))
        assertTrue(body.contains("activateNewLiveMatch("))
        assertTrue(body.contains("isLocalScoreEmptyForLiveMatchBootstrap()"))
        assertTrue(body.contains("bootstrapLocalScoreFromLiveMatch("))
        assertTrue(body.contains("adoptBootstrappedLiveScoreIfNeeded("))
        assertTrue(body.contains("saveCurrentLiveMatchId()"))
        assertFalse(body.contains("previousLiveMatchId == null &&"))
    }

    @Test
    fun newLiveMatchClearsFinishedLifecycleBeforeItBecomesCurrent() {
        val body =
            applyLiveMatchResponseBody()

        assertTrue(body.contains("lastFinishedMatchId != match.matchId"))
        assertTrue(body.contains("matchLifecycleState == MatchLifecycleState.FINISHED_BY_ARENA"))
        assertTrue(body.contains("activateNewLiveMatch("))
        assertTrue(
            body.indexOf("activateNewLiveMatch(") <
                    body.indexOf("currentLiveMatchId =")
        )
        assertTrue(
            body.indexOf("activateNewLiveMatch(") <
                    body.indexOf("match.sideA.label")
        )
        assertTrue(
            body.indexOf("activateNewLiveMatch(") <
                    body.indexOf("bootstrapLocalScoreFromLiveMatch(")
        )
        assertTrue(body.contains("replacesFinishedMatch ||"))
    }

    @Test
    fun matchNullPreservesFinishedLifecycleForReopen() {
        val body =
            applyLiveMatchResponseBody()
        val nullBranch =
            body.substring(
                body.indexOf("if (match == null)"),
                body.indexOf("arenaLifecycleSequenceStore.advanceToAtLeast(")
            )

        assertFalse(nullBranch.contains("activateNewLiveMatch("))
        assertFalse(nullBranch.contains("clearMatchLifecycleState("))
        assertFalse(nullBranch.contains("lastFinishedMatchId = null"))
        assertFalse(nullBranch.contains("lastFinishedSnapshot = null"))
        assertFalse(nullBranch.contains("matchLifecycleState ="))
        assertFalse(nullBranch.contains("currentLiveMatchId = null"))
    }

    @Test
    fun activateNewLiveMatchInvalidatesOldLifecycleWithoutTouchingBleOrClients() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun activateNewLiveMatch(",
                endMarker = "private fun updateTeamLabels("
            )

        assertTrue(body.contains("clearMatchLifecycleState()"))
        assertTrue(body.contains("localMatchFinished = false"))
        assertTrue(body.contains("scoreHistory.clear()"))
        assertFalse(body.contains("deviceA ="))
        assertFalse(body.contains("deviceB ="))
        assertFalse(body.contains("stopBleScan("))
        assertFalse(body.contains("startBleScan("))
        assertFalse(body.contains("arenaAuthClient"))
        assertFalse(body.contains("arenaCourtSelection ="))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("ArenaRealScoreSync"))
    }

    @Test
    fun emptyLocalScoreDefinitionIsExplicit() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun isLocalScoreEmptyForLiveMatchBootstrap()",
                endMarker = "private fun bootstrapLocalScoreFromLiveMatch("
            )

        assertTrue(body.contains("pointsA == 0"))
        assertTrue(body.contains("pointsB == 0"))
        assertTrue(body.contains("gamesA == 0"))
        assertTrue(body.contains("gamesB == 0"))
        assertTrue(body.contains("setsA == 0"))
        assertTrue(body.contains("setsB == 0"))
        assertTrue(body.contains("scoreHistory.isEmpty()"))
        assertTrue(body.contains("matchLifecycleState == MatchLifecycleState.ACTIVE"))
    }

    @Test
    fun bootstrapCopiesOfficialGamesAndSetsWithoutPointSideEffects() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun bootstrapLocalScoreFromLiveMatch(",
                endMarker = "private fun adoptBootstrappedLiveScoreIfNeeded("
            )

        assertTrue(body.contains("gamesA ="))
        assertTrue(body.contains("match.scoreA.coerceAtLeast("))
        assertTrue(body.contains("gamesB ="))
        assertTrue(body.contains("match.scoreB.coerceAtLeast("))
        assertTrue(body.contains("setsA ="))
        assertTrue(body.contains("match.setsA.coerceAtLeast("))
        assertTrue(body.contains("setsB ="))
        assertTrue(body.contains("match.setsB.coerceAtLeast("))
        assertTrue(body.contains("pointsA = 0"))
        assertTrue(body.contains("pointsB = 0"))
        assertTrue(body.contains("advantageSide = null"))
        assertTrue(body.contains("tieBreakActive = false"))
        assertTrue(body.contains("tieBreakPointsA = 0"))
        assertTrue(body.contains("tieBreakPointsB = 0"))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("arenaRealScoreSync"))
        assertFalse(body.contains("arenaSequenceStore.nextSequence()"))
        assertFalse(body.contains("announce"))
        assertFalse(body.contains("flashScore"))
    }

    @Test
    fun matchChangeClearsOldHistoryAndSameMatchDoesNotInventHistory() {
        val applyBody =
            applyLiveMatchResponseBody()
        val bootstrapBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun bootstrapLocalScoreFromLiveMatch(",
                endMarker = "private fun adoptBootstrappedLiveScoreIfNeeded("
            )

        assertTrue(applyBody.contains("clearPreviousHistory = matchChanged"))
        assertTrue(bootstrapBody.contains("if (clearPreviousHistory)"))
        assertTrue(bootstrapBody.contains("scoreHistory.clear()"))
        assertFalse(bootstrapBody.contains("scoreHistory.add("))
    }

    @Test
    fun currentLiveMatchIdIsPersistedAcrossRestart() {
        val source =
            mainActivitySource()
        val loadSaveBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadSavedData()",
                endMarker = "private fun updateScreen()"
            )

        assertTrue(source.contains("PREF_CURRENT_LIVE_MATCH_ID"))
        assertTrue(loadSaveBody.contains("currentLiveMatchId ="))
        assertTrue(loadSaveBody.contains("PREF_CURRENT_LIVE_MATCH_ID"))
        assertTrue(source.contains("private fun saveCurrentLiveMatchId()"))
        assertTrue(source.contains("PREF_LAST_ADOPTED_LIVE_SCORE_KEY"))
        assertTrue(source.contains("putString("))
    }

    @Test
    fun adoptRunsOnlyAfterBootstrapAndIsDeduplicatedByMatchScore() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun adoptBootstrappedLiveScoreIfNeeded(",
                endMarker = "private fun liveScoreAdoptionKey("
            )

        assertTrue(body.contains("if (standaloneClassicMode)"))
        assertTrue(body.contains("matchLifecycleState != MatchLifecycleState.ACTIVE"))
        assertTrue(body.contains("arenaLiveScoreAdoptionClient ?: return"))
        assertTrue(body.contains("lastAdoptedLiveScoreKey == adoptionKey"))
        assertTrue(body.contains("ArenaLiveScoreAdoptionRequest("))
        assertTrue(body.contains("UUID.randomUUID().toString()"))
        assertTrue(body.contains("arenaSequenceStore.nextSequence()"))
        assertTrue(body.contains("adoptionClient.adoptLiveScore("))
        assertTrue(body.contains("lastAdoptedLiveScoreKey ="))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("arenaRealScoreSync"))
    }

    @Test
    fun adoptFailureDoesNotAlterLocalScore() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun adoptBootstrappedLiveScoreIfNeeded(",
                endMarker = "private fun liveScoreAdoptionKey("
            )
        val failureBranch =
            body.substring(
                body.indexOf("} else {"),
                body.indexOf("private fun", 1).takeIf { it > 0 } ?: body.length
            )

        assertTrue(failureBranch.contains("Baseline Arena non aggiornata"))
        assertFalse(failureBranch.contains("pointsA ="))
        assertFalse(failureBranch.contains("gamesA ="))
        assertFalse(failureBranch.contains("saveState()"))
    }

    @Test
    fun standaloneAndMatchNullDoNotBootstrapOrClearScore() {
        val body =
            applyLiveMatchResponseBody()
        val nullBranch =
            body.substring(
                body.indexOf("if (match == null)"),
                body.indexOf("val previousLiveMatchId")
            )

        assertTrue(body.contains("if (standaloneClassicMode)"))
        assertTrue(nullBranch.contains("DEFAULT_TEAM_A_LABEL"))
        assertTrue(nullBranch.contains("DEFAULT_TEAM_B_LABEL"))
        assertFalse(nullBranch.contains("currentLiveMatchId = null"))
        assertFalse(nullBranch.contains("pointsA ="))
        assertFalse(nullBranch.contains("gamesA ="))
        assertFalse(nullBranch.contains("saveState()"))
        assertFalse(nullBranch.contains("bootstrapLocalScoreFromLiveMatch"))
    }

    @Test
    fun configureDoesNotDropPersistedMatchIdDuringNormalStartup() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun configureArenaCourtClients(",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertTrue(body.contains("val previousSelectedCourtId"))
        assertTrue(body.contains("previousSelectedCourtId != null"))
        assertTrue(body.contains("previousSelectedCourtId != selection.selectedCourtId"))
        assertTrue(body.contains("currentLiveMatchId = null"))
    }

    private fun applyLiveMatchResponseBody(): String {
        return methodSlice(
            source = mainActivitySource(),
            startMarker = "private fun applyLiveMatchResponse(",
            endMarker = "private fun updateTeamLabels("
        )
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
