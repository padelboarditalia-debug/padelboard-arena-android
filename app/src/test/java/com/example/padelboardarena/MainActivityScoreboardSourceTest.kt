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

        assertEquals(
            2,
            Regex(
                """android:textSize="320sp"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeMinTextSize="140sp"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeMaxTextSize="380sp"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:singleLine="true"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:maxLines="1"""
            ).findAll(layout).count()
        )
        assertEquals(
            2,
            Regex(
                """android:autoSizeTextType="uniform"""
            ).findAll(layout).count()
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
        assertTrue(layout.contains("""android:layout_width="56dp"""))
        assertTrue(layout.contains("""android:layout_height="56dp"""))
        assertTrue(layout.contains("""android:layout_gravity="bottom|start"""))
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

        assertTrue(source.contains("private const val NUMERIC_SCORE_TEXT_SIZE_SP = 320"))
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
                endMarker = "private fun winGame("
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
