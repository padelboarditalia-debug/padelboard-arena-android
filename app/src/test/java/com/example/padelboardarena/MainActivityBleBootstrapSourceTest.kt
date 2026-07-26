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

    private fun hasAnyShellyAssociationBody(): String {
        return methodBody(
            "private fun hasAnyShellyAssociation()",
            "private fun startBleScanIfDevicesAssigned()"
        )
    }

    private fun startBleScanIfDevicesAssignedBody(): String {
        return methodBody(
            "private fun startBleScanIfDevicesAssigned()",
            "private fun beginAssignment("
        )
    }

    private fun methodBody(
        startMarker: String,
        endMarker: String
    ): String {
        val source =
            File(
                "src/main/java/com/example/padelboardarena/MainActivity.kt"
            ).readText()

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