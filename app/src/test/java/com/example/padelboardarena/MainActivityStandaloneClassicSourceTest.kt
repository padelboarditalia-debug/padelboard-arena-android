package com.example.padelboardarena

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityStandaloneClassicSourceTest {
    @Test
    fun operationModeStoreDefaultsToConnectedMode() {
        val source =
            File(
                "src/main/java/com/example/padelboardarena/arena/ArenaOperationModeStore.kt"
            ).readText()

        assertTrue(source.contains("arena_standalone_classic_mode"))
        assertTrue(source.contains("getBoolean("))
        assertTrue(source.contains("false"))
        assertFalse(source.contains("true"))
    }

    @Test
    fun settingsExposeStandaloneToggleAndLocalReset() {
        val layout =
            settingsLayout()
        val source =
            settingsActivitySource()

        assertTrue(layout.contains("arenaSettingsStandaloneClassicModeCheckBox"))
        assertTrue(layout.contains("Modalità autonoma Game classico"))
        assertTrue(layout.contains("arenaSettingsResetScoreButton"))
        assertTrue(layout.contains("Azzera punteggio"))
        assertTrue(source.contains("REQUEST_OPERATION_MODE_CHANGED"))
        assertTrue(source.contains("REQUEST_RESET_SCORE"))
        assertTrue(source.contains("ArenaOperationModeStore("))
        assertTrue(source.contains("setStandaloneClassicMode("))
    }

    @Test
    fun standaloneModeDisablesCourtFetchFromSettings() {
        val source =
            settingsActivitySource()
        val updateBody =
            methodSlice(
                source = source,
                startMarker = "private fun updateCourtSelectionUi()",
                endMarker = "private fun fallbackArenaCourtSelection()"
            )
        val loadBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadArenaCourtsForSelection()",
                endMarker = "private fun showCourtSelectionDialog("
            )

        assertTrue(updateBody.contains("arenaOperationModeStore.isStandaloneClassicMode()"))
        assertTrue(updateBody.contains("Collegamento PadelBoard disattivato"))
        assertTrue(updateBody.contains("selectCourtButton.isEnabled = false"))
        assertTrue(loadBody.contains("arenaOperationModeStore.isStandaloneClassicMode()"))
        assertTrue(loadBody.indexOf("arenaOperationModeStore.isStandaloneClassicMode()") < loadBody.indexOf("arenaCourtsClient.fetchCourtsBlocking()"))
    }

    @Test
    fun mainReloadsStandaloneModeAndHandlesResetRequest() {
        val source =
            mainActivitySource()
        val launcherBody =
            methodSlice(
                source = source,
                startMarker = "private val arenaSettingsLauncher =",
                endMarker = "private val permissionLauncher ="
            )

        assertTrue(source.contains("private fun reloadArenaOperationMode()"))
        assertTrue(launcherBody.contains("reloadArenaOperationMode()"))
        assertTrue(launcherBody.contains("ArenaSettingsActivity.REQUEST_RESET_SCORE"))
        assertTrue(launcherBody.contains("resetMatch()"))
    }

    @Test
    fun standaloneModeDoesNotCreateBackendClientsOrPolling() {
        val source =
            mainActivitySource()
        val configureBody =
            methodSlice(
                source = source,
                startMarker = "private fun configureArenaCourtClients(",
                endMarker = "private fun applyLiveMatchResponse("
            )
        val pollingBody =
            methodSlice(
                source = source,
                startMarker = "private fun startLiveMatchPolling()",
                endMarker = "private fun stopLiveMatchPolling()"
            )

        assertTrue(configureBody.contains("if (standaloneClassicMode)"))
        assertTrue(configureBody.indexOf("if (standaloneClassicMode)") < configureBody.indexOf("ArenaApiClient("))
        assertTrue(configureBody.contains("arenaApiClient = null"))
        assertTrue(configureBody.contains("arenaLiveMatchClient = null"))
        assertTrue(configureBody.contains("arenaRealScoreSync = null"))
        assertTrue(pollingBody.contains("if (standaloneClassicMode)"))
        assertTrue(pollingBody.indexOf("if (standaloneClassicMode)") < pollingBody.indexOf("liveMatchPollingActive = true"))
    }

    @Test
    fun standaloneModeDoesNotPostArenaSnapshots() {
        val source =
            mainActivitySource()
        val enqueueBody =
            methodSlice(
                source = source,
                startMarker = "private fun enqueueArenaSnapshot()",
                endMarker = "private fun hasAnyShellyAssociation()"
            )

        assertTrue(enqueueBody.contains("if (standaloneClassicMode)"))
        assertTrue(enqueueBody.indexOf("if (standaloneClassicMode)") < enqueueBody.indexOf("sync.enqueue("))
        assertTrue(enqueueBody.contains("Modalità autonoma"))
    }

    @Test
    fun standaloneRegisterPointUsesClassicScoringOnlyBeforeConnectedLogic() {
        val source =
            mainActivitySource()
        val registerBody =
            methodSlice(
                source = source,
                startMarker = "private fun registerPoint(",
                endMarker = "private fun announceValidPoint("
            )
        val standaloneBody =
            methodSlice(
                source = source,
                startMarker = "private fun registerStandaloneClassicPoint(",
                endMarker = "private fun teamLabelForSide("
            )

        assertTrue(registerBody.contains("if (standaloneClassicMode)"))
        assertTrue(registerBody.contains("registerStandaloneClassicPoint("))
        assertTrue(registerBody.indexOf("registerStandaloneClassicPoint(") < registerBody.indexOf("saveStateUpdateScreenAndEnqueueArenaSnapshot()"))
        assertTrue(standaloneBody.contains("ArenaClassicScoring.registerPoint("))
        assertTrue(standaloneBody.contains("localMatchFinished"))
    }

    @Test
    fun localStatePersistsSetsTieBreakAndFinishedFlag() {
        val source =
            mainActivitySource()
        val loadSaveBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadSavedData()",
                endMarker = "private fun updateScreen()"
            )

        assertTrue(source.contains("PREF_SETS_A"))
        assertTrue(source.contains("PREF_SETS_B"))
        assertTrue(source.contains("PREF_TIE_BREAK_ACTIVE"))
        assertTrue(source.contains("PREF_TIE_BREAK_POINTS_A"))
        assertTrue(source.contains("PREF_TIE_BREAK_POINTS_B"))
        assertTrue(source.contains("PREF_LOCAL_MATCH_FINISHED"))
        assertTrue(loadSaveBody.contains("setsA = preferences.getInt("))
        assertTrue(loadSaveBody.contains("putInt("))
        assertTrue(loadSaveBody.contains("putBoolean("))
    }

    @Test
    fun undoRestoresLocalSetAndTieBreakStateWithoutStandalonePost() {
        val source =
            mainActivitySource()
        val snapshotBody =
            methodSlice(
                source = source,
                startMarker = "data class ScoreSnapshot(",
                endMarker = "enum class ButtonEvent("
            )
        val undoBody =
            methodSlice(
                source = source,
                startMarker = "private fun undoLastAction()",
                endMarker = "private fun resetMatch()"
            )

        assertTrue(snapshotBody.contains("val setsA: Int"))
        assertTrue(snapshotBody.contains("val setsB: Int"))
        assertTrue(snapshotBody.contains("val tieBreakActive: Boolean"))
        assertTrue(snapshotBody.contains("val localMatchFinished: Boolean"))
        assertTrue(undoBody.contains("snapshot.setsA"))
        assertTrue(undoBody.contains("snapshot.tieBreakActive"))
        assertTrue(undoBody.contains("snapshot.localMatchFinished"))
        assertTrue(undoBody.contains("if (standaloneClassicMode)"))
        assertTrue(undoBody.contains("saveState()"))
        assertTrue(undoBody.contains("updateScreen()"))
    }

    @Test
    fun resetMatchClearsOnlyLocalScoreFields() {
        val source =
            mainActivitySource()
        val resetBody =
            methodSlice(
                source = source,
                startMarker = "private fun resetMatch()",
                endMarker = "private fun resetDeviceAssignments()"
            )

        assertTrue(resetBody.contains("pointsA = 0"))
        assertTrue(resetBody.contains("gamesA = 0"))
        assertTrue(resetBody.contains("setsA = 0"))
        assertTrue(resetBody.contains("tieBreakActive = false"))
        assertTrue(resetBody.contains("tieBreakPointsA = 0"))
        assertTrue(resetBody.contains("localMatchFinished = false"))
        assertTrue(resetBody.contains("scoreHistory.clear()"))
        assertFalse(resetBody.contains("enqueueArenaSnapshot("))
        assertFalse(resetBody.contains("arenaRealScoreSync"))
    }

    @Test
    fun longPressFinishAndDoubleTapReopenAreNotImplementedInPhaseA() {
        val source =
            mainActivitySource()
        val eventBody =
            methodSlice(
                source = source,
                startMarker = "private fun handleButtonEvent(",
                endMarker = "private fun registerPoint("
            )

        assertTrue(eventBody.contains("ButtonEvent.LONG_PRESS"))
        assertTrue(eventBody.contains("Nessuna azione configurata"))
        assertFalse(eventBody.contains("finishMatch"))
        assertFalse(eventBody.contains("reopen"))
    }

    private fun mainActivitySource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/MainActivity.kt"
        ).readText()
    }

    private fun settingsActivitySource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/ArenaSettingsActivity.kt"
        ).readText()
    }

    private fun settingsLayout(): String {
        return File(
            "src/main/res/layout/activity_arena_settings.xml"
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
