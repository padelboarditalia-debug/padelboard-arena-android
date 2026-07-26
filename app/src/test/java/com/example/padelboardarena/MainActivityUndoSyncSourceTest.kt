package com.example.padelboardarena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityUndoSyncSourceTest {
    @Test
    fun undoValidBranchUsesArenaSnapshotFinalizerOnce() {
        val undoBody =
            undoLastActionBody()

        assertEquals(
            1,
            Regex(
                "saveStateUpdateScreenAndEnqueueArenaSnapshot\\s*\\("
            ).findAll(undoBody).count()
        )
    }

    @Test
    fun undoEmptyHistoryReturnsBeforeArenaSnapshotFinalizer() {
        val undoBody =
            undoLastActionBody()

        val emptyHistoryReturnIndex =
            undoBody.indexOf(
                "return"
            )

        val finalizerIndex =
            undoBody.indexOf(
                "saveStateUpdateScreenAndEnqueueArenaSnapshot"
            )

        assertTrue(emptyHistoryReturnIndex >= 0)
        assertTrue(finalizerIndex > emptyHistoryReturnIndex)
    }

    private fun undoLastActionBody(): String {
        val source =
            File(
                "src/main/java/com/example/padelboardarena/MainActivity.kt"
            ).readText()

        val methodStart =
            source.indexOf(
                "private fun undoLastAction()"
            )

        val nextMethodStart =
            source.indexOf(
                "private fun resetMatch()",
                methodStart
            )

        return source.substring(
            methodStart,
            nextMethodStart
        )
    }
}
