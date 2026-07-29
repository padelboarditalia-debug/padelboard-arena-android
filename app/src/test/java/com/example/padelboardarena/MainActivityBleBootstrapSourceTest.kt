package com.example.padelboardarena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class MainActivityBleBootstrapSourceTest {
    @Test
    fun sideAAssociationRequestsBleScan() {
        val helperBody =
            hasAnyShellyAssociationBody()

        assertTrue(
            helperBody.contains(
                "deviceA != null"
            )
        )
    }

    @Test
    fun sideBAssociationRequestsBleScan() {
        val helperBody =
            hasAnyShellyAssociationBody()

        assertTrue(
            helperBody.contains(
                "deviceB != null"
            )
        )
    }

    @Test
    fun missingAssociationDoesNotRequestBleScan() {
        val bootstrapBody =
            startBleScanIfDevicesAssignedBody()

        assertTrue(
            Regex(
                "if\\s*\\(\\s*!hasAnyShellyAssociation\\s*\\(\\s*\\)\\s*\\)\\s*\\{\\s*return\\s*\\}"
            ).containsMatchIn(
                bootstrapBody
            )
        )
    }

    @Test
    fun activeScanDoesNotStartSecondScan() {
        val bootstrapBody =
            startBleScanIfDevicesAssignedBody()

        assertTrue(
            Regex(
                "if\\s*\\(\\s*scanning\\s*\\)\\s*\\{\\s*return\\s*\\}"
            ).containsMatchIn(
                bootstrapBody
            )
        )

        assertEquals(
            1,
            Regex(
                "checkPermissionsAndStart\\s*\\("
            ).findAll(
                bootstrapBody
            ).count()
        )
    }

    @Test
    fun bootstrapDoesNotEnterAssignmentMode() {
        val bootstrapBody =
            startBleScanIfDevicesAssignedBody()

        assertFalse(
            bootstrapBody.contains(
                "assignmentMode ="
            )
        )
    }

    @Test
    fun beginAssignmentStillStartsScanWhenNeeded() {
        val beginAssignmentBody =
            methodBody(
                "private fun beginAssignment(",
                "private fun checkPermissionsAndStart()"
            )

        assertTrue(
            Regex(
                "if\\s*\\(\\s*!scanning\\s*\\)\\s*\\{\\s*checkPermissionsAndStart\\s*\\(\\s*\\)\\s*\\}"
            ).containsMatchIn(
                beginAssignmentBody
            )
        )
    }

    @Test
    fun onCreateBootstrapsBleAfterSavedStateIsRendered() {
        val onCreateBody =
            methodBody(
                "override fun onCreate(",
                "private fun bindViews()"
            )

        val loadIndex =
            onCreateBody.indexOf(
                "loadSavedData()"
            )
        val updateIndex =
            onCreateBody.indexOf(
                "updateScreen()"
            )
        val bootstrapIndex =
            onCreateBody.indexOf(
                "startBleScanIfDevicesAssigned()"
            )

        assertTrue(loadIndex >= 0)
        assertTrue(updateIndex > loadIndex)
        assertTrue(bootstrapIndex > updateIndex)
    }

    @Test
    fun onResumeRestartsScanWhenAssignmentsExistAndWatchdogStarts() {
        val source =
            mainActivitySource()
        val onResumeBody =
            methodBody(
                "override fun onResume()",
                "override fun onStop()"
            )

        assertTrue(source.contains("override fun onResume()"))
        assertTrue(onResumeBody.contains("startBleScanIfDevicesAssigned()"))
        assertTrue(onResumeBody.contains("startBleWatchdogIfNeeded()"))
    }

    @Test
    fun bleWatchdogUsesMonotonicTimestampsAndConfiguredThresholds() {
        val source =
            mainActivitySource()
        val watchdogBody =
            methodBody(
                "private fun runBleWatchdogCheck()",
                "private fun restartBleScanSafely("
            )

        assertTrue(source.contains("private const val BLE_WATCHDOG_INTERVAL_MS = 30_000L"))
        assertTrue(source.contains("private const val BLE_INACTIVITY_RESTART_MS = 180_000L"))
        assertTrue(source.contains("private const val BLE_MIN_SCAN_AGE_BEFORE_RESTART_MS = 180_000L"))
        assertTrue(source.contains("SystemClock.elapsedRealtime()"))
        assertTrue(watchdogBody.contains("!hasAnyShellyAssociation()"))
        assertTrue(watchdogBody.contains("assignmentMode != null"))
        assertTrue(watchdogBody.contains("bleScanRestartPending"))
        assertTrue(watchdogBody.contains("!isBluetoothReadyForScan()"))
        assertTrue(watchdogBody.contains("!hasBleScanPermissions()"))
        assertTrue(watchdogBody.contains("restartBleScanSafely("))
    }

    @Test
    fun watchdogDoesNotRunWithoutAssignmentsOrDuringAssignmentMode() {
        val startWatchdogBody =
            methodBody(
                "private fun startBleWatchdogIfNeeded()",
                "private fun stopBleWatchdog()"
            )
        val watchdogBody =
            methodBody(
                "private fun runBleWatchdogCheck()",
                "private fun restartBleScanSafely("
            )

        assertTrue(startWatchdogBody.contains("!hasAnyShellyAssociation()"))
        assertTrue(startWatchdogBody.contains("return"))
        assertTrue(watchdogBody.contains("assignmentMode != null"))
        assertTrue(watchdogBody.contains("return"))
    }

    @Test
    fun restartBleScanSafelyPreventsConcurrentRestartsAndUsesCanonicalStart() {
        val body =
            methodBody(
                "private fun restartBleScanSafely(",
                "private fun beginAssignment("
            )

        assertTrue(body.contains("bleScanRestartPending"))
        assertTrue(body.contains("!activityVisible"))
        assertTrue(body.contains("!hasAnyShellyAssociation()"))
        assertTrue(body.contains("assignmentMode != null"))
        assertTrue(body.contains("!isBluetoothReadyForScan()"))
        assertTrue(body.contains("!hasBleScanPermissions()"))
        assertTrue(body.contains("stopBleScanInternal("))
        assertTrue(body.contains("clearAssignment = false"))
        assertTrue(body.contains("postDelayed("))
        assertTrue(body.contains("checkPermissionsAndStart()"))
        assertFalse(body.contains("registerPoint("))
        assertFalse(body.contains("enqueueArenaSnapshot("))
        assertFalse(body.contains("ArenaRealScoreSync"))
    }

    @Test
    fun scanFailureClearsScanningAndSchedulesOneControlledRetry() {
        val body =
            methodBody(
                "override fun onScanFailed(",
                "private fun processScanResult("
            )

        assertTrue(body.contains("scanning = false"))
        assertTrue(body.contains("bleScanStartedAt = 0L"))
        assertTrue(body.contains("BLE scan failed: \$errorCode"))
        assertTrue(body.contains("restartBleScanSafely("))
        assertTrue(body.contains("BLE_SCAN_FAILURE_RETRY_DELAY_MS"))
    }

    @Test
    fun restartedScanStillDispatchesSinglePressToNormalScoringPath() {
        val restartBody =
            methodBody(
                "private fun restartBleScanSafely(",
                "private fun beginAssignment("
            )
        val scanCallbackBody =
            methodBody(
                "private val scanCallback =",
                "private fun processScanResult("
            )
        val processBody =
            methodBody(
                "private fun processScanResult(",
                "private fun extractShellyMac("
            )
        val handleBody =
            methodBody(
                "private fun handleButtonEvent(",
                "private fun correctGameForSide("
            )

        assertTrue(restartBody.contains("checkPermissionsAndStart()"))
        assertTrue(scanCallbackBody.contains("processScanResult(result)"))
        assertTrue(processBody.contains("handleButtonEvent("))
        assertTrue(handleBody.contains("ButtonEvent.SINGLE_PRESS"))
        assertTrue(handleBody.contains("registerPoint(side)"))
    }

    @Test
    fun bleDiagnosticsCoverCallbackParseDedupButtonAndDispatchWithoutSecrets() {
        val source =
            mainActivitySource()
        val processBody =
            methodBody(
                "private fun processScanResult(",
                "private fun extractShellyMac("
            )
        val handleBody =
            methodBody(
                "private fun handleButtonEvent(",
                "private fun correctGameForSide("
            )
        val registerBody =
            methodBody(
                "private fun registerPoint(",
                "private fun registerStandaloneClassicPoint("
            )

        assertTrue(source.contains("BLE app callback: onScanResult"))
        assertTrue(source.contains("maskedBleDeviceId("))
        assertTrue(processBody.contains("BLE packet received=no"))
        assertTrue(processBody.contains("BLE packet received=yes"))
        assertTrue(processBody.contains("associated="))
        assertTrue(processBody.contains("BLE parse valid="))
        assertTrue(processBody.contains("BLE button event: device="))
        assertTrue(processBody.contains("rawCode="))
        assertTrue(processBody.contains("parsed="))
        assertTrue(processBody.contains("unsupported_button_code"))
        assertTrue(processBody.contains("packetId="))
        assertTrue(processBody.contains("buttonEvent="))
        assertTrue(processBody.contains("BLE dedup accept: packet_id"))
        assertTrue(processBody.contains("BLE dedup reject: duplicate_packet_id"))
        assertTrue(source.contains("mutableMapOf<String, BlePacketDedupKey>()"))
        assertTrue(processBody.contains("rawButtonEventCode = parsedPacket.rawButtonEventCode"))
        assertTrue(processBody.contains("previousPacketId == dedupKey"))
        assertTrue(processBody.contains("BLE dedup accept: fallback"))
        assertTrue(processBody.contains("BLE dedup reject: fallback_window"))
        assertTrue(processBody.contains("dispatchTarget"))
        assertTrue(processBody.contains("\"finish_match\""))
        assertTrue(processBody.contains("BLE dispatch: buttonEvent="))
        assertTrue(handleBody.contains("BLE scoring dispatch=no reason=unassociated"))
        assertTrue(registerBody.contains("BLE scoring dispatch=no reason=lifecycle"))
        assertTrue(registerBody.contains("BLE scoring dispatch=yes side="))
        val diagnosticBodies =
            processBody + handleBody + registerBody

        assertFalse(diagnosticBodies.contains("Authorization"))
        assertFalse(diagnosticBodies.contains("access token"))
        assertFalse(diagnosticBodies.contains("refresh token"))
        assertFalse(diagnosticBodies.contains("password"))
    }

    @Test
    fun longPressDiagnosticsAndDispatchReachFinishWithoutChangingOtherGestures() {
        val source =
            mainActivitySource()
        val processBody =
            methodBody(
                "private fun processScanResult(",
                "private fun extractShellyMac("
            )
        val handleBody =
            methodBody(
                "private fun handleButtonEvent(",
                "private fun correctGameForSide("
            )

        assertTrue(source.contains("LONG_PRESS(0x04)"))
        assertTrue(source.contains("HOLD_PRESS(0x80)"))
        assertTrue(source.contains("data class BlePacketDedupKey"))
        assertTrue(processBody.contains("rawButtonEventCode"))
        assertTrue(processBody.contains("BlePacketDedupKey("))
        assertTrue(processBody.contains("dispatchTarget"))
        assertTrue(processBody.contains("\"finish_match\""))
        assertTrue(handleBody.contains("ButtonEvent.SINGLE_PRESS ->"))
        assertTrue(handleBody.contains("registerPoint(side)"))
        assertTrue(handleBody.contains("ButtonEvent.DOUBLE_PRESS ->"))
        assertTrue(handleBody.contains("handleDoublePress()"))
        assertTrue(handleBody.contains("ButtonEvent.TRIPLE_PRESS ->"))
        assertTrue(handleBody.contains("correctGameForSide(side)"))
        assertTrue(handleBody.contains("ButtonEvent.LONG_PRESS,"))
        assertTrue(handleBody.contains("ButtonEvent.HOLD_PRESS ->"))
        assertTrue(handleBody.contains("handleLongPress()"))
    }

    @Test
    fun bluetoothStateReceiverHandlesOffAndOnWithoutTouchingScore() {
        val body =
            methodBody(
                "private val bluetoothStateReceiver =",
                "private val pointFlashColor ="
            )

        assertTrue(body.contains("BluetoothAdapter.ACTION_STATE_CHANGED"))
        assertTrue(body.contains("BluetoothAdapter.STATE_OFF"))
        assertTrue(body.contains("scanning = false"))
        assertTrue(body.contains("Bluetooth non disponibile"))
        assertTrue(body.contains("BluetoothAdapter.STATE_ON"))
        assertTrue(body.contains("restartBleScanSafely("))
        assertFalse(body.contains("pointsA ="))
        assertFalse(body.contains("gamesA ="))
        assertFalse(body.contains("saveState()"))
    }

    @Test
    fun onStopAndDestroyRemoveBleWatchdogCallbacks() {
        val source =
            mainActivitySource()
        val onStopBody =
            methodBody(
                "override fun onStop()",
                "private fun bindViews()"
            )
        val onDestroyBody =
            methodBody(
                "override fun onDestroy()",
                "super.onDestroy()"
            )
        val stopWatchdogBody =
            methodBody(
                "private fun stopBleWatchdog()",
                "private fun scheduleNextBleWatchdogCheck()"
            )

        assertTrue(onStopBody.contains("stopBleWatchdog()"))
        assertTrue(onDestroyBody.contains("stopBleWatchdog()"))
        assertTrue(onDestroyBody.contains("unregisterBluetoothStateReceiver()"))
        assertTrue(stopWatchdogBody.contains("removeCallbacks"))
        assertTrue(source.contains("registerBluetoothStateReceiver()"))
    }

    @Test
    fun packetTimestampsAreUpdatedWithoutChangingParserOrDedupOrder() {
        val body =
            methodBody(
                "private fun processScanResult(",
                "private fun extractShellyMac("
            )

        val packetTimestampIndex =
            body.indexOf("lastBlePacketReceivedAt")
        val parseIndex =
            body.indexOf("parseShellyPacket(")
        val buttonTimestampIndex =
            body.indexOf("lastBleButtonEventAt")
        val dedupIndex =
            body.indexOf("lastPacketIdByDevice[")
        val dispatchIndex =
            body.indexOf("handleButtonEvent(")

        assertTrue(packetTimestampIndex >= 0)
        assertTrue(parseIndex > packetTimestampIndex)
        assertTrue(buttonTimestampIndex > parseIndex)
        assertTrue(dedupIndex > buttonTimestampIndex)
        assertTrue(dispatchIndex > dedupIndex)
        assertTrue(body.contains("parseShellyPacket("))
        assertTrue(body.contains("lastPacketIdByDevice["))
        assertTrue(body.contains("lastFallbackEventByDevice["))
    }

    private fun mainActivitySource(): String {
        return File(
            "src/main/java/com/example/padelboardarena/MainActivity.kt"
        ).readText()
    }

    private fun hasAnyShellyAssociationBody(): String {
        return methodBody(
            "private fun hasAnyShellyAssociation()",
            "private fun startBleScanIfDevicesAssigned()"
        )
    }

    private fun startBleScanIfDevicesAssignedBody(): String {
        return methodBody(
            "private fun startBleScanIfDevicesAssigned()",
            "private fun startBleWatchdogIfNeeded()"
        )
    }

    private fun methodBody(
        startMarker: String,
        endMarker: String
    ): String {
        val source =
            mainActivitySource()

        val methodStart =
            source.indexOf(
                startMarker
            )

        val nextMethodStart =
            source.indexOf(
                endMarker,
                methodStart
            )

        assertTrue(methodStart >= 0)
        assertTrue(nextMethodStart > methodStart)

        return source.substring(
            methodStart,
            nextMethodStart
        )
    }
}
