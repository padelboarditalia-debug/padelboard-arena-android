package com.example.padelboardarena

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArenaSettingsSourceTest {
    @Test
    fun mainLayoutDoesNotExposePasswordOrManualLoginButton() {
        val layout =
            mainLayout()

        assertFalse(
            layout.contains(
                "arenaPasswordEditText"
            )
        )
        assertFalse(
            layout.contains(
                "Password E2E"
            )
        )
        assertFalse(
            layout.contains(
                "arenaLoginButton"
            )
        )
        assertFalse(
            layout.contains(
                "android:text=\"Login Arena\""
            )
        )
    }

    @Test
    fun settingsLayoutContainsPasswordAndLogin() {
        val layout =
            settingsLayout()

        assertTrue(
            layout.contains(
                "arenaSettingsPasswordEditText"
            )
        )
        assertTrue(
            layout.contains(
                "Password E2E"
            )
        )
        assertTrue(
            layout.contains(
                "arenaSettingsLoginButton"
            )
        )
        assertTrue(
            layout.contains(
                "android:text=\"Login Arena\""
            )
        )
    }

    @Test
    fun settingsLoginUsesSecureSessionStore() {
        val source =
            settingsActivitySource()

        assertTrue(
            source.contains(
                "ArenaAuthClient("
            )
        )
        assertTrue(
            source.contains(
                "AndroidKeystoreArenaSessionStore"
            )
        )
        assertTrue(
            source.contains(
                "ArenaBuildConfig.load()"
            )
        )
        assertTrue(
            source.contains(
                "arenaAuthClient.login("
            )
        )
    }

    @Test
    fun returningToMainRestoresPersistedSession() {
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
                "ActivityResultContracts.StartActivityForResult()"
            )
        )
        assertTrue(
            launcherBody.contains(
                "bootstrapArenaSession()"
            )
        )
    }

    @Test
    fun validSessionShowsArenaConnected() {
        val bootstrapBody =
            bootstrapArenaSessionBody()

        assertTrue(
            bootstrapBody.contains(
                "ArenaSessionRestoreResult.RESTORED"
            )
        )
        assertTrue(
            bootstrapBody.contains(
                "Arena connessa"
            )
        )
    }

    @Test
    fun failedOrMissingRefreshShowsLoginRequired() {
        val bootstrapBody =
            bootstrapArenaSessionBody()

        assertTrue(
            bootstrapBody.contains(
                "ArenaSessionRestoreResult.FAILED"
            )
        )
        assertTrue(
            bootstrapBody.contains(
                "ArenaSessionRestoreResult.MISSING"
            )
        )
        assertTrue(
            Regex(
                "Login Arena richiesto"
            ).findAll(
                bootstrapBody
            ).count() >= 2
        )
    }

    @Test
    fun tokenAndPasswordAreNotDisplayedByUiMessages() {
        val messagesSource =
            File(
                "src/main/java/com/example/padelboardarena/arena/ArenaManualUiMessages.kt"
            ).readText()

        assertFalse(
            messagesSource.contains(
                "access_token"
            )
        )
        assertFalse(
            messagesSource.contains(
                "refresh_token"
            )
        )
        assertFalse(
            messagesSource.contains(
                "password"
            )
        )
    }

    @Test
    fun shellySyncStillUsesExistingArenaClasses() {
        val source =
            mainActivitySource()

        assertTrue(
            source.contains(
                "ArenaRealScoreSync("
            )
        )
        assertTrue(
            source.contains(
                "ArenaRealScoreSnapshotFactory("
            )
        )
        assertTrue(
            source.contains(
                "ArenaApiScoreSnapshotSender("
            )
        )
        assertTrue(
            source.contains(
                "enqueueArenaSnapshot()"
            )
        )
    }

    private fun bootstrapArenaSessionBody(): String {
        return methodSlice(
            source = mainActivitySource(),
            startMarker = "private fun bootstrapArenaSession()",
            endMarker = "private fun openArenaSettings()"
        )
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

    private fun mainLayout(): String {
        return File(
            "src/main/res/layout/activity_main.xml"
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
