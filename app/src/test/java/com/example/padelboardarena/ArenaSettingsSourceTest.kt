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
    fun settingsLayoutContainsArenaCourtSelectionSection() {
        val layout =
            settingsLayout()

        assertTrue(layout.contains("Campo associato"))
        assertTrue(layout.contains("arenaSettingsCourtStatusText"))
        assertTrue(layout.contains("Seleziona campo Arena"))
        assertTrue(layout.contains("arenaSettingsSelectCourtButton"))
        assertTrue(layout.contains("android:text=\"Seleziona campo\""))
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
                "handleSettingsAssignmentRequest("
            )
        )
        assertTrue(
            source.contains(
                "private fun handleSettingsAssignmentRequest("
            )
        )
        assertTrue(
            source.contains(
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
                "Attendi un secondo, poi premi il pulsante Shelly lato \$sideName"
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
    fun assignmentArmingGuardRunsBeforeDeduplicationAndScoring() {
        val source =
            mainActivitySource()
        val processBody =
            methodSlice(
                source = source,
                startMarker = "private fun processScanResult(",
                endMarker = "private fun extractShellyMac("
            )
        val guardIndex =
            processBody.indexOf(
                "System.currentTimeMillis() < assignmentArmedAt"
            )
        val guardStartIndex =
            processBody.indexOf(
                "currentAssignment != null"
            )
        val packetDedupIndex =
            processBody.indexOf(
                "lastPacketIdByDevice["
            )
        val fallbackDedupIndex =
            processBody.indexOf(
                "lastFallbackEventByDevice["
            )
        val dispatchIndex =
            processBody.indexOf(
                "handleButtonEvent("
            )

        assertTrue(guardIndex >= 0)
        assertTrue(guardStartIndex >= 0)
        assertTrue(guardIndex > guardStartIndex)
        assertTrue(packetDedupIndex > guardIndex)
        assertTrue(fallbackDedupIndex > guardIndex)
        assertTrue(dispatchIndex > guardIndex)

        val prematureGuard =
            processBody.substring(
                guardStartIndex,
                packetDedupIndex
            )

        assertTrue(
            prematureGuard.contains(
                "currentAssignment != null"
            )
        )
        assertTrue(
            prematureGuard.contains(
                "return"
            )
        )
        assertFalse(
            prematureGuard.contains(
                "lastPacketIdByDevice["
            )
        )
        assertFalse(
            prematureGuard.contains(
                "lastFallbackEventByDevice["
            )
        )
        assertFalse(
            prematureGuard.contains(
                "assignDevice("
            )
        )
        assertFalse(
            prematureGuard.contains(
                "registerPoint("
            )
        )
        assertFalse(
            prematureGuard.contains(
                "arenaRealScoreSync"
            )
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
    fun settingsLoadsArenaCourtsAndSavesSelection() {
        val source =
            settingsActivitySource()

        assertTrue(source.contains("ArenaCourtsClient("))
        assertTrue(source.contains("ArenaCourtSelectionStore("))
        assertTrue(source.contains("loadArenaCourtsForSelection()"))
        assertTrue(source.contains("arenaCourtsClient.fetchCourtsBlocking()"))
        assertTrue(source.contains("arenaCourtSelectionStore.saveSelection("))
        assertTrue(source.contains("REQUEST_COURT_CHANGED"))
    }

    @Test
    fun inactiveCourtIsShownButNotSelectable() {
        val source =
            settingsActivitySource()
        val dialogBody =
            methodSlice(
                source = source,
                startMarker = "private fun showCourtSelectionDialog(",
                endMarker = "private fun formatCourtSelectionLabel("
            )

        assertTrue(dialogBody.contains("if (!court.isActive)"))
        assertTrue(dialogBody.contains("Campo non attivo"))
        assertTrue(dialogBody.indexOf("if (!court.isActive)") < dialogBody.indexOf("arenaCourtSelectionStore.saveSelection("))
    }

    @Test
    fun unmappedCourtIsMarkedAsNotReady() {
        val source =
            settingsActivitySource()
        val labelBody =
            methodSlice(
                source = source,
                startMarker = "private fun formatCourtSelectionLabel(",
                endMarker = "private fun updateShellyAssignmentUi()"
            )

        assertTrue(source.contains("Non collegato a un campo torneo"))
        assertTrue(labelBody.contains("non pronto"))
        assertTrue(labelBody.contains("collegato a"))
    }

    @Test
    fun courtSelectionUsesFallbackBuildConfigOnlyWhenStoreIsEmpty() {
        val source =
            settingsActivitySource()
        val updateBody =
            methodSlice(
                source = source,
                startMarker = "private fun updateCourtSelectionUi()",
                endMarker = "private fun loadArenaCourtsForSelection()"
            )

        assertTrue(updateBody.contains("arenaCourtSelectionStore.readSelection()"))
        assertTrue(updateBody.contains("?: fallbackArenaCourtSelection()"))
        assertTrue(source.contains("arenaConfig.courtId.trim()"))
    }

    @Test
    fun settingsCourtSelectionDoesNotExposeTokens() {
        val source =
            settingsActivitySource()
        val courtBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadArenaCourtsForSelection()",
                endMarker = "private fun showCourtSelectionDialog("
            )

        assertFalse(courtBody.contains("accessToken"))
        assertFalse(courtBody.contains("refreshToken"))
        assertFalse(courtBody.contains("Bearer"))
        assertFalse(courtBody.contains("result.body"))
    }

    @Test
    fun courtLoadFailureShowsSafeMessageAndKeepsSelection() {
        val source =
            settingsActivitySource()
        val loadBody =
            methodSlice(
                source = source,
                startMarker = "private fun loadArenaCourtsForSelection()",
                endMarker = "private fun showCourtSelectionDialog("
            )

        assertTrue(loadBody.contains("Impossibile caricare i campi Arena"))
        assertTrue(loadBody.contains("selectCourtButton.isEnabled = true"))
        assertFalse(loadBody.contains("arenaCourtSelectionStore.saveSelection("))
        assertFalse(loadBody.contains("setResult("))
        assertFalse(loadBody.contains("finish()"))
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
