package com.example.padelboardarena

import org.junit.Assert.assertEquals
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
    fun settingsLayoutContainsShellyButtonSections() {
        val layout =
            settingsLayout()

        assertTrue(
            layout.contains(
                "Pulsanti Shelly"
            )
        )
        assertTrue(
            layout.contains(
                "arenaSettingsDeviceAStatusText"
            )
        )
        assertTrue(
            layout.contains(
                "arenaSettingsAssignAButton"
            )
        )
        assertTrue(
            layout.contains(
                "arenaSettingsDeviceBStatusText"
            )
        )
        assertTrue(
            layout.contains(
                "arenaSettingsAssignBButton"
            )
        )
    }

    @Test
    fun settingsLayoutShowsAssociatedAndMissingStates() {
        val layout =
            settingsLayout()
        val source =
            settingsActivitySource()

        assertTrue(
            layout.contains(
                "Pulsante A: non associato"
            )
        )
        assertTrue(
            layout.contains(
                "Pulsante B: non associato"
            )
        )
        assertTrue(
            source.contains(
                "\$statusLabel: associato"
            )
        )
        assertTrue(
            source.contains(
                "\$statusLabel: non associato"
            )
        )
        assertTrue(
            source.contains(
                "Sostituisci \$assignLabel"
            )
        )
        assertTrue(
            source.contains(
                "Associa \$assignLabel"
            )
        )
    }

    @Test
    fun settingsAssignmentButtonsReturnSideRequests() {
        val source =
            settingsActivitySource()

        assertTrue(
            source.contains(
                "const val REQUEST_ASSIGN_SIDE_A = \"ASSIGN_SIDE_A\""
            )
        )
        assertTrue(
            source.contains(
                "const val REQUEST_ASSIGN_SIDE_B = \"ASSIGN_SIDE_B\""
            )
        )
        assertTrue(
            Regex(
                """assignAButton\.setOnClickListener\s*\{\s*returnAssignmentRequest\(\s*REQUEST_ASSIGN_SIDE_A\s*\)\s*\}"""
            ).containsMatchIn(
                source
            )
        )
        assertTrue(
            Regex(
                """assignBButton\.setOnClickListener\s*\{\s*returnAssignmentRequest\(\s*REQUEST_ASSIGN_SIDE_B\s*\)\s*\}"""
            ).containsMatchIn(
                source
            )
        )
        assertTrue(
            source.contains(
                "Intent().putExtra("
            )
        )
        assertTrue(
            source.contains(
                "EXTRA_SETTINGS_REQUEST"
            )
        )
        assertTrue(
            source.contains(
                "setResult("
            )
        )
    }

    @Test
    fun mainReceivesSettingsAssignmentRequests() {
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
                "ArenaSettingsActivity.EXTRA_SETTINGS_REQUEST"
            )
        )
        assertTrue(
            launcherBody.contains(
                "ArenaSettingsActivity.REQUEST_ASSIGN_SIDE_A"
            )
        )
        assertTrue(
            launcherBody.contains(
                "ArenaSettingsActivity.REQUEST_ASSIGN_SIDE_B"
            )
        )
        assertTrue(
            launcherBody.contains(
                "requestShellyAssignment("
            )
        )
        assertTrue(
            launcherBody.contains(
                "AssignmentMode.SIDE_A"
            )
        )
        assertTrue(
            launcherBody.contains(
                "AssignmentMode.SIDE_B"
            )
        )
    }

    @Test
    fun mainAssignmentRequestDelegatesToExistingBeginAssignment() {
        val source =
            mainActivitySource()
        val helperBody =
            methodSlice(
                source = source,
                startMarker = "private fun requestShellyAssignment(",
                endMarker = "private fun showArenaApiDiagnostic("
            )

        assertTrue(
            helperBody.contains(
                "Premi il pulsante Shelly lato \$sideName"
            )
        )
        assertEquals(
            1,
            Regex(
                """beginAssignment\s*\(\s*mode\s*\)"""
            ).findAll(
                helperBody
            ).count()
        )
    }

    @Test
    fun settingsDoesNotContainBleScannerOrParsing() {
        val source =
            settingsActivitySource()

        assertFalse(
            source.contains(
                "BluetoothLeScanner"
            )
        )
        assertFalse(
            source.contains(
                "ScanCallback"
            )
        )
        assertFalse(
            source.contains(
                "ScanRecord"
            )
        )
        assertFalse(
            source.contains(
                "BTHome"
            )
        )
        assertFalse(
            source.contains(
                "packetId"
            )
        )
        assertFalse(
            source.contains(
                "assignDevice("
            )
        )
        assertFalse(
            source.contains(
                "checkPermissionsAndStart("
            )
        )
    }

    @Test
    fun mainLayoutDoesNotContainFixedShellyAssignmentButtons() {
        val layout =
            mainLayout()

        assertFalse(
            layout.contains(
                "arenaSettingsAssignAButton"
            )
        )
        assertFalse(
            layout.contains(
                "arenaSettingsAssignBButton"
            )
        )
        assertFalse(
            layout.contains(
                "Associa pulsante A"
            )
        )
        assertFalse(
            layout.contains(
                "Associa pulsante B"
            )
        )
    }

    @Test
    fun bleBootstrapAndShellyPersistenceKeysStayUnchanged() {
        val source =
            mainActivitySource()
        val settingsSource =
            settingsActivitySource()

        assertTrue(
            source.contains(
                "private const val PREF_DEVICE_A = \"device_a\""
            )
        )
        assertTrue(
            source.contains(
                "private const val PREF_DEVICE_B = \"device_b\""
            )
        )
        assertTrue(
            settingsSource.contains(
                "private const val PREF_DEVICE_A = \"device_a\""
            )
        )
        assertTrue(
            settingsSource.contains(
                "private const val PREF_DEVICE_B = \"device_b\""
            )
        )
        assertTrue(
            source.contains(
                "startBleScanIfDevicesAssigned()"
            )
        )
        assertTrue(
            source.contains(
                "if (!hasAnyShellyAssociation())"
            )
        )
        assertTrue(
            source.contains(
                "if (scanning)"
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
    fun settingsLoginShowsDistinctProgressSuccessAndFailureMessages() {
        val source =
            settingsActivitySource()
        val messagesSource =
            File(
                "src/main/java/com/example/padelboardarena/arena/ArenaManualUiMessages.kt"
            ).readText()

        assertTrue(
            source.contains(
                "Arena: login in corso"
            )
        )
        assertTrue(
            source.contains(
                "Arena connessa"
            )
        )
        assertTrue(
            messagesSource.contains(
                "Login Arena fallito"
            )
        )
        assertFalse(
            methodSlice(
                source = messagesSource,
                startMarker = "fun loginFailed(",
                endMarker = "fun safeLoginFailureCause("
            ).contains(
                "Login Arena richiesto"
            )
        )
    }

    @Test
    fun settingsLoginLogsAreSanitizedAndKeepLoginCall() {
        val source =
            settingsActivitySource()

        assertTrue(
            source.contains(
                "Arena login start"
            )
        )
        assertTrue(
            source.contains(
                "Arena login success"
            )
        )
        assertTrue(
            source.contains(
                "Arena login failed: \${error.javaClass.simpleName} - "
            )
        )
        assertTrue(
            source.contains(
                "ArenaManualUiMessages.safeLoginFailureCause("
            )
        )
        assertTrue(
            Regex(
                """arenaAuthClient\.login\s*\(\s*password\s*=\s*password\s*\)"""
            ).containsMatchIn(
                source
            )
        )
        assertTrue(
            source.contains(
                "loginButton.setOnClickListener"
            )
        )
        assertTrue(
            source.contains(
                "runArenaLogin()"
            )
        )
        assertFalse(
            source.contains(
                "accessToken"
            )
        )
        assertFalse(
            source.contains(
                "refreshToken"
            )
        )
        assertFalse(
            source.contains(
                "supabasePublishableKey"
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
