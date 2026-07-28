package com.example.padelboardarena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityScoreboardSourceTest {
    @Test
    fun mainLayoutContainsTwoDistinctSides() {
        val layout =
            mainLayout()

        assertTrue(
            layout.contains(
                "sideAPanel"
            )
        )
        assertTrue(
            layout.contains(
                "sideBPanel"
            )
        )
        assertTrue(
            layout.contains(
                "Squadra A"
            )
        )
        assertTrue(
            layout.contains(
                "Squadra B"
            )
        )
    }

    @Test
    fun pointsUseSeparateViewsForBothSides() {
        val layout =
            mainLayout()

        assertTrue(
            layout.contains(
                "scoreAText"
            )
        )
        assertTrue(
            layout.contains(
                "scoreBText"
            )
        )
    }

    @Test
    fun scoreTextIsLargerAndAutoSizedOnOneLine() {
        val layout =
            mainLayout()
        val scoreABody =
            viewSlice(
                source = layout,
                startMarker = "scoreAText",
                endMarker = "GAME"
            )
        val scoreBBody =
            viewSlice(
                source = layout,
                startMarker = "scoreBText",
                endMarker = "GAME"
            )

        assertEquals(
            2,
            Regex(
                """android:textSize="360sp"""
            ).findAll(scoreABody + scoreBBody).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeMinTextSize="140sp"""
            ).findAll(scoreABody + scoreBBody).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeMaxTextSize="430sp"""
            ).findAll(scoreABody + scoreBBody).count()
        )
        assertEquals(
            2,
            Regex(
                """android:singleLine="true"""
            ).findAll(scoreABody + scoreBBody).count()
        )
        assertEquals(
            2,
            Regex(
                """android:maxLines="1"""
            ).findAll(scoreABody + scoreBBody).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeTextType="uniform"""
            ).findAll(scoreABody + scoreBBody).count()
        )
    }

    @Test
    fun scoreValuesRemainSingleLineCompatible() {
        val layout =
            mainLayout()
        val supportedValues =
            listOf(
                "0",
                "15",
                "30",
                "40",
                "ADV"
            )

        assertTrue(layout.contains("""android:singleLine="true"""))
        assertTrue(layout.contains("""android:maxLines="1"""))
        assertFalse(layout.contains("android:ellipsize"))
        supportedValues.forEach { value ->
            assertTrue(value.isNotBlank())
        }
    }

    @Test
    fun teamNamesUseUniformAutosizeOnOneLine() {
        val layout =
            mainLayout()
        val teamABody =
            viewSlice(
                source = layout,
                startMarker = "teamAText",
                endMarker = "scoreAText"
            )
        val teamBBody =
            viewSlice(
                source = layout,
                startMarker = "teamBText",
                endMarker = "scoreBText"
            )

        listOf(
            teamABody,
            teamBBody
        ).forEach { body ->
            assertTrue(body.contains("android:textSize=\"42sp\""))
            assertTrue(body.contains("android:singleLine=\"true\""))
            assertTrue(body.contains("android:maxLines=\"1\""))
            assertTrue(body.contains("android:autoSizeTextType=\"uniform\""))
            assertTrue(body.contains("android:autoSizeMinTextSize=\"20sp\""))
            assertTrue(body.contains("android:autoSizeMaxTextSize=\"42sp\""))
            assertTrue(body.contains("android:autoSizeStepGranularity=\"2sp\""))
            assertTrue(body.contains("android:includeFontPadding=\"false\""))
            assertFalse(body.contains("android:ellipsize"))
        }
    }

    @Test
    fun gameAndSetViewsArePresentForBothSides() {
        val layout =
            mainLayout()

        assertTrue(layout.contains("gamesAText"))
        assertTrue(layout.contains("gamesBText"))
        assertTrue(layout.contains("setsAText"))
        assertTrue(layout.contains("setsBText"))
    }

    @Test
    fun gameAndSetValuesAreLargerButRemainSecondaryToPoints() {
        val layout =
            mainLayout()

        assertEquals(
            2,
            Regex(
                """android:textSize="58sp"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:textSize="52sp"""
            ).findAll(layout).count()
        )
        assertEquals(
            4,
            Regex(
                """android:textSize="16sp"""
            ).findAll(layout).count()
        )
        assertTrue(
            layout.indexOf(
                "gamesAText"
            ) < layout.indexOf(
                """android:textSize="58sp""",
                layout.indexOf(
                    "gamesAText"
                )
            )
        )
    }

    @Test
    fun scoreboardUsesReducedSymmetricSpacing() {
        val layout =
            mainLayout()

        assertTrue(
            layout.contains(
                """android:padding="4dp"""
            )
        )
        assertTrue(
            layout.contains(
                """android:paddingTop="0dp"""
            )
        )
        assertTrue(
            layout.contains(
                """android:paddingBottom="0dp"""
            )
        )
        assertEquals(
            1,
            Regex(
                """android:paddingEnd="4dp"""
            ).findAll(layout).count()
        )
        assertEquals(
            1,
            Regex(
                """android:paddingStart="4dp"""
            ).findAll(layout).count()
        )
    }

    @Test
    fun mainLayoutDoesNotContainPasswordOrLoginArena() {
        val layout =
            mainLayout()

        assertFalse(layout.contains("arenaPasswordEditText"))
        assertFalse(layout.contains("arenaLoginButton"))
        assertFalse(layout.contains("Password E2E"))
        assertFalse(layout.contains("""android:text="Login Arena" />"""))
    }

    @Test
    fun arenaSettingsIconButtonRemainsAvailableFromMain() {
        val layout =
            mainLayout()
        val source =
            mainActivitySource()

        assertTrue(layout.contains("arenaSettingsButton"))
        assertTrue(layout.contains("<ImageButton"))
        assertTrue(layout.contains("""android:src="@drawable/ic_settings"""))
        assertTrue(layout.contains("""android:contentDescription="Impostazioni Arena"""))
        assertTrue(layout.contains("""android:layout_width="64dp"""))
        assertTrue(layout.contains("""android:layout_height="64dp"""))
        assertTrue(layout.contains("""android:layout_gravity="bottom|end"""))
        assertFalse(layout.contains("""android:text="Impostazioni Arena"""))
        assertTrue(source.contains("openArenaSettings()"))
        assertTrue(source.contains("ArenaSettingsActivity::class.java"))
        assertTrue(source.contains("private lateinit var arenaSettingsButton: ImageButton"))
    }

    @Test
    fun settingsIconDrawableExists() {
        val drawable =
            File(
                "src/main/res/drawable/ic_settings.xml"
            ).readText()

        assertTrue(drawable.contains("<vector"))
        assertTrue(drawable.contains("android:pathData"))
    }

    @Test
    fun advantageScoreUsesReducedSizingAndNumbersRestoreLargeSizing() {
        val source =
            mainActivitySource()
        val updateBody =
            methodSlice(
                source = source,
                startMarker = "private fun updateScreen()",
                endMarker = "private fun applyScoreTextSizing("
            )
        val sizingBody =
            methodSlice(
                source = source,
                startMarker = "private fun applyScoreTextSizing(",
                endMarker = "private fun displayPointForSide("
            )

        assertTrue(source.contains("private const val NUMERIC_SCORE_TEXT_SIZE_SP = 360"))
        assertTrue(source.contains("private const val ADV_SCORE_TEXT_SIZE_SP = 200"))
        assertTrue(sizingBody.contains("displayedValue == \"ADV\""))
        assertTrue(sizingBody.contains("ADV_SCORE_TEXT_SIZE_SP"))
        assertTrue(sizingBody.contains("NUMERIC_SCORE_TEXT_SIZE_SP"))
        assertTrue(sizingBody.contains("setAutoSizeTextTypeUniformWithConfiguration"))
        assertTrue(sizingBody.contains("TypedValue.COMPLEX_UNIT_SP"))
        assertTrue(
            Regex(
                """applyScoreTextSizing\s*\(\s*textView\s*=\s*scoreAText,\s*displayedValue\s*=\s*displayedScoreA\s*\)"""
            ).containsMatchIn(updateBody)
        )
        assertTrue(
            Regex(
                """applyScoreTextSizing\s*\(\s*textView\s*=\s*scoreBText,\s*displayedValue\s*=\s*displayedScoreB\s*\)"""
            ).containsMatchIn(updateBody)
        )
    }

    @Test
    fun scoreSizingHelperDoesNotInvokeScoringOrArenaSync() {
        val sizingBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun applyScoreTextSizing(",
                endMarker = "private fun displayPointForSide("
            )

        assertFalse(sizingBody.contains("registerPoint("))
        assertFalse(sizingBody.contains("undoLastAction("))
        assertFalse(sizingBody.contains("saveState()"))
        assertFalse(sizingBody.contains("enqueueArenaSnapshot()"))
        assertFalse(sizingBody.contains("ArenaRealScoreSync"))
    }

    @Test
    fun validPointBranchesTriggerOneGreenAnimationAfterStateUpdate() {
        val registerBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun registerPoint(",
                endMarker = "private fun announceValidPoint("
            )

        val stateUpdateCount =
            Regex(
                """saveStateUpdateScreenAndEnqueueArenaSnapshot\s*\("""
            ).findAll(registerBody).count()
        val animationCount =
            Regex(
                """animatePointChange\s*\("""
            ).findAll(registerBody).count()

        assertTrue(stateUpdateCount > 0)
        assertEquals(stateUpdateCount, animationCount)
    }

    @Test
    fun validUndoTriggersOneRedAnimation() {
        val undoBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun undoLastAction()",
                endMarker = "private fun resetMatch()"
            )

        assertEquals(
            1,
            Regex(
                """animateUndoChange\s*\("""
            ).findAll(undoBody).count()
        )
        assertTrue(
            undoBody.contains(
                "snapshot.scoringSide"
            )
        )
    }

    @Test
    fun triplePressDelegatesToGameCorrection() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun handleButtonEvent(",
                endMarker = "private fun correctGameForSide("
            )

        assertTrue(body.contains("ButtonEvent.TRIPLE_PRESS"))
        assertTrue(body.contains("correctGameForSide(side)"))
        assertFalse(body.contains("finishCurrentMatch()"))
        assertFalse(body.contains("reopenFinishedMatch()"))
    }

    @Test
    fun gameCorrectionChangesOnlyOneGameAndKeepsPointStateUntouched() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun correctGameForSide(",
                endMarker = "private fun handleDoublePress()"
            )

        assertTrue(body.contains("matchLifecycleState !="))
        assertTrue(body.contains("MatchLifecycleState.ACTIVE"))
        assertTrue(body.contains("localMatchFinished"))
        assertTrue(body.contains("currentGames <= 0"))
        assertTrue(body.contains("Nessun game da correggere"))
        assertTrue(
            body.indexOf("currentGames <= 0") <
                    body.indexOf("saveSnapshot(")
        )
        assertTrue(body.contains("gamesA - 1"))
        assertTrue(body.contains("gamesB - 1"))
        assertTrue(body.contains("coerceAtLeast("))
        assertTrue(body.contains("saveSnapshot("))
        assertTrue(body.contains("saveStateUpdateScreenAndEnqueueArenaSnapshot()"))
        assertTrue(body.contains("saveState()"))
        assertTrue(body.contains("animateGameCorrectionChange("))
        assertTrue(body.contains("announceGameCorrection("))
        assertFalse(Regex("""pointsA\s*=""").containsMatchIn(body))
        assertFalse(Regex("""pointsB\s*=""").containsMatchIn(body))
        assertFalse(Regex("""setsA\s*=""").containsMatchIn(body))
        assertFalse(Regex("""setsB\s*=""").containsMatchIn(body))
        assertFalse(Regex("""tieBreakActive\s*=""").containsMatchIn(body))
        assertFalse(Regex("""tieBreakPointsA\s*=""").containsMatchIn(body))
        assertFalse(Regex("""tieBreakPointsB\s*=""").containsMatchIn(body))
        assertFalse(body.contains("finishCurrentMatch("))
        assertFalse(body.contains("reopenFinishedMatch("))
    }

    @Test
    fun gameCorrectionUsesSeparateRedGameAnimation() {
        val body =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun animateGameCorrectionChange(",
                endMarker = "private fun animateScoreChange("
            )

        assertTrue(body.contains("gamesAText"))
        assertTrue(body.contains("gamesBText"))
        assertTrue(body.contains("gameAnimationA"))
        assertTrue(body.contains("gameAnimationB"))
        assertTrue(body.contains("undoFlashColor"))
        assertTrue(body.contains("standardColor"))
        assertFalse(body.contains("scoreAText"))
        assertFalse(body.contains("scoreBText"))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("registerPoint("))
    }

    @Test
    fun emptyUndoReturnsBeforeAnimation() {
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
        val animationIndex =
            undoBody.indexOf(
                "animateUndoChange"
            )

        assertTrue(emptyReturnIndex >= 0)
        assertTrue(animationIndex > emptyReturnIndex)
    }

    @Test
    fun greenAndRedAnimationsUseSameSlowFiveSecondRhythm() {
        val source =
            mainActivitySource()
        val pointBody =
            methodSlice(
                source = source,
                startMarker = "private fun animatePointChange(",
                endMarker = "private fun animateUndoChange("
            )
        val undoBody =
            methodSlice(
                source = source,
                startMarker = "private fun animateUndoChange(",
                endMarker = "private fun animateScoreChange("
            )
        val animationBody =
            animationBody()

        assertTrue(source.contains("private const val SCORE_FLASH_DURATION_MS = 5_000L"))
        assertTrue(source.contains("private const val SCORE_FLASH_PULSE_COUNT = 5"))
        assertTrue(source.contains("private const val SCORE_FLASH_RISE_MS = 350L"))
        assertTrue(source.contains("private const val SCORE_FLASH_HOLD_MS = 200L"))
        assertTrue(source.contains("private const val SCORE_FLASH_FALL_MS = 350L"))
        assertTrue(source.contains("private const val SCORE_FLASH_PAUSE_MS = 100L"))
        assertTrue(animationBody.contains("repeat(SCORE_FLASH_PULSE_COUNT)"))
        assertTrue(animationBody.contains("playSequentially"))
        assertTrue(pointBody.contains("pointFlashColor"))
        assertTrue(undoBody.contains("undoFlashColor"))
    }

    @Test
    fun animationCancelsPreviousAndAlwaysEndsWhite() {
        val animationBody =
            animationBody()

        assertTrue(animationBody.contains("previousAnimation?.cancel()"))
        assertTrue(animationBody.contains("onAnimationEnd"))
        assertTrue(animationBody.contains("onAnimationCancel"))
        assertTrue(
            Regex(
                """target\.setTextColor\s*\(\s*Color\.WHITE\s*\)"""
            ).containsMatchIn(animationBody)
        )
    }

    @Test
    fun animationHasFiveHeartbeatPulses() {
        val animationBody =
            animationBody()

        assertTrue(animationBody.contains("AnimatorSet"))
        assertTrue(animationBody.contains("ObjectAnimator.ofArgb"))
        assertEquals(
            4,
            Regex(
                """duration = SCORE_FLASH_(RISE|HOLD|FALL|PAUSE)_MS"""
            ).findAll(animationBody).count()
        )
        assertTrue(animationBody.contains("Color.WHITE,"))
        assertTrue(animationBody.contains("flashColor,"))
    }

    @Test
    fun animationHelperDoesNotInvokeScoringOrArenaSync() {
        val animationBody =
            animationBody()

        assertFalse(animationBody.contains("registerPoint("))
        assertFalse(animationBody.contains("undoLastAction("))
        assertFalse(animationBody.contains("saveState()"))
        assertFalse(animationBody.contains("enqueueArenaSnapshot()"))
        assertFalse(animationBody.contains("ArenaRealScoreSync"))
    }

    @Test
    fun mainActivityIsLandscapeAndKeepsScreenAwake() {
        val manifest =
            File(
                "src/main/AndroidManifest.xml"
            ).readText()
        val source =
            mainActivitySource()

        assertTrue(manifest.contains("""android:screenOrientation="landscape"""))
        assertTrue(source.contains("ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE"))
        assertTrue(source.contains("WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON"))
    }

    @Test
    fun liveMatchPollingUpdatesLabelsWithoutTouchingScoringOrSync() {
        val source =
            mainActivitySource()
        val liveMatchBody =
            methodSlice(
                source = source,
                startMarker = "private fun refreshLiveMatchIfNeeded()",
                endMarker = "private fun reloadArenaCourtSelection()"
            )

        assertTrue(source.contains("ArenaLiveMatchClient"))
        assertTrue(source.contains("private lateinit var teamAText: TextView"))
        assertTrue(source.contains("private lateinit var teamBText: TextView"))
        assertTrue(liveMatchBody.contains("applyLiveMatchResponse"))
        assertTrue(source.contains("private fun updateTeamLabels("))
        assertTrue(source.contains("teamAText.text"))
        assertTrue(source.contains("teamBText.text"))
        assertFalse(liveMatchBody.contains("registerPoint("))
        assertFalse(liveMatchBody.contains("undoLastAction("))
        assertFalse(liveMatchBody.contains("saveState()"))
        assertFalse(liveMatchBody.contains("enqueueArenaSnapshot()"))
        assertFalse(liveMatchBody.contains("ArenaRealScoreSync"))
    }

    @Test
    fun liveMatchNullUsesFallbackLabelsAndMessage() {
        val liveMatchBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun applyLiveMatchResponse(",
                endMarker = "private fun updateTeamLabels("
            )

        assertTrue(liveMatchBody.contains("match == null"))
        assertTrue(liveMatchBody.contains("DEFAULT_TEAM_A_LABEL"))
        assertTrue(liveMatchBody.contains("DEFAULT_TEAM_B_LABEL"))
        assertTrue(liveMatchBody.contains("Nessuna partita live"))
        assertFalse(liveMatchBody.contains("pointsA ="))
        assertFalse(liveMatchBody.contains("pointsB ="))
        assertFalse(liveMatchBody.contains("gamesA ="))
        assertFalse(liveMatchBody.contains("gamesB ="))
    }

    @Test
    fun liveMatchPollingAvoidsOverlappingRequests() {
        val refreshBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun refreshLiveMatchIfNeeded()",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertTrue(refreshBody.contains("if (liveMatchRequestInFlight)"))
        assertTrue(refreshBody.contains("return"))
        assertTrue(refreshBody.contains("liveMatchRequestInFlight = true"))
        assertTrue(refreshBody.contains("liveMatchRequestInFlight = false"))
        assertTrue(refreshBody.contains("liveMatchClient.fetchLiveMatch"))
    }

    @Test
    fun liveMatchPollingStopsWhenActivityIsNotVisible() {
        val source =
            mainActivitySource()
        val stopBody =
            methodSlice(
                source = source,
                startMarker = "private fun stopLiveMatchPolling()",
                endMarker = "private fun scheduleNextLiveMatchPoll()"
            )

        assertTrue(source.contains("override fun onStart()"))
        assertTrue(source.contains("startLiveMatchPolling()"))
        assertTrue(source.contains("override fun onStop()"))
        assertTrue(source.contains("stopLiveMatchPolling()"))
        assertTrue(stopBody.contains("liveMatchPollingActive = false"))
        assertTrue(stopBody.contains("removeCallbacks"))
        assertTrue(source.contains("arenaLiveMatchClient?.shutdown()"))
    }

    @Test
    fun missingArenaCourtSelectionDoesNotStartPollingOrPost() {
        val source =
            mainActivitySource()
        val pollingBody =
            methodSlice(
                source = source,
                startMarker = "private fun startLiveMatchPolling()",
                endMarker = "private fun stopLiveMatchPolling()"
            )
        val enqueueBody =
            methodSlice(
                source = source,
                startMarker = "private fun enqueueArenaSnapshot()",
                endMarker = "private fun hasAnyShellyAssociation()"
            )

        assertTrue(pollingBody.contains("arenaCourtSelection == null"))
        assertTrue(pollingBody.contains("arenaLiveMatchClient == null"))
        assertTrue(pollingBody.contains("liveMatchPollingActive = false"))
        assertTrue(pollingBody.contains("Seleziona campo Arena"))
        assertTrue(enqueueBody.contains("arenaRealScoreSync"))
        assertTrue(enqueueBody.contains("if (sync == null)"))
        assertTrue(enqueueBody.contains("Seleziona campo Arena"))
    }

    @Test
    fun fallbackBuildConfigCourtIsUsedOnlyWhenSelectionStoreIsEmpty() {
        val source =
            mainActivitySource()
        val reloadBody =
            methodSlice(
                source = source,
                startMarker = "private fun reloadArenaCourtSelection()",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertTrue(source.contains("ArenaCourtSelectionStore("))
        assertTrue(reloadBody.contains("arenaCourtSelectionStore.readSelection()"))
        assertTrue(reloadBody.contains("?: fallbackArenaCourtSelection()"))
        assertTrue(reloadBody.contains("arenaConfig.courtId.trim()"))
        assertTrue(reloadBody.contains("selectedCourtId = courtId"))
    }

    @Test
    fun courtChangeRecreatesPostLiveMatchAndSyncTogether() {
        val source =
            mainActivitySource()
        val configureBody =
            methodSlice(
                source = source,
                startMarker = "private fun configureArenaCourtClients(",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertTrue(configureBody.contains("stopLiveMatchPolling()"))
        assertTrue(configureBody.contains("liveMatchClientGeneration += 1"))
        assertTrue(configureBody.contains("arenaApiClient?.shutdown()"))
        assertTrue(configureBody.contains("arenaLiveMatchClient?.shutdown()"))
        assertTrue(configureBody.contains("arenaConfig.copy("))
        assertTrue(configureBody.contains("courtId = selection.selectedCourtId"))
        assertTrue(configureBody.contains("ArenaApiClient("))
        assertTrue(configureBody.contains("ArenaLiveMatchClient("))
        assertTrue(configureBody.contains("ArenaRealScoreSync("))
        assertTrue(configureBody.contains("ArenaApiScoreSnapshotSender("))
    }

    @Test
    fun oldLiveMatchResponsesAreIgnoredAfterCourtChange() {
        val refreshBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun refreshLiveMatchIfNeeded()",
                endMarker = "private fun reloadArenaCourtSelection()"
            )

        assertTrue(refreshBody.contains("val requestGeneration"))
        assertTrue(refreshBody.contains("liveMatchClientGeneration"))
        assertTrue(refreshBody.contains("requestGeneration != liveMatchClientGeneration"))
        assertTrue(refreshBody.contains("return@runOnUiThread"))
    }

    @Test
    fun courtChangeDoesNotResetScoreOrShellyAssociations() {
        val configureBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun configureArenaCourtClients(",
                endMarker = "private fun applyLiveMatchResponse("
            )

        assertFalse(configureBody.contains("pointsA ="))
        assertFalse(configureBody.contains("pointsB ="))
        assertFalse(configureBody.contains("gamesA ="))
        assertFalse(configureBody.contains("gamesB ="))
        assertFalse(configureBody.contains("deviceA ="))
        assertFalse(configureBody.contains("deviceB ="))
        assertFalse(configureBody.contains("resetMatch()"))
        assertFalse(configureBody.contains("resetDeviceAssignments()"))
    }

    @Test
    fun liveMatchNamesAreUsedByVoiceFormatter() {
        val updateLabelsBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun updateTeamLabels(",
                endMarker = "private fun openArenaSettings()"
            )

        assertTrue(updateLabelsBody.contains("scoreAnnouncer.updateTeamLabels"))
        assertTrue(updateLabelsBody.contains("teamALabel ="))
        assertTrue(updateLabelsBody.contains("teamBLabel ="))
    }

    @Test
    fun liveMatchUiAndLogsDoNotExposeTokensOrBodies() {
        val liveMatchBody =
            methodSlice(
                source = mainActivitySource(),
                startMarker = "private fun refreshLiveMatchIfNeeded()",
                endMarker = "private fun reloadArenaCourtSelection()"
            )

        assertFalse(liveMatchBody.contains("result.body"))
        assertFalse(liveMatchBody.contains("Authorization"))
        assertFalse(liveMatchBody.contains("Bearer"))
        assertFalse(liveMatchBody.contains("refreshToken"))
    }

    private fun mainActivitySource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/MainActivity.kt"
        ).readText()
    }

    private fun mainLayout(): String {
        return File(
            "src/main/res/layout/activity_main.xml"
        ).readText()
    }

    private fun animationBody(): String {
        return methodSlice(
            source = mainActivitySource(),
            startMarker = "private fun animateScoreChange(",
            endMarker = "private fun registerPoint("
        )
    }

    private fun viewSlice(
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
