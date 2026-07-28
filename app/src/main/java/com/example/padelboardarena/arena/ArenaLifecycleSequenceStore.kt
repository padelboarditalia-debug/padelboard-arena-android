package com.example.padelboardarena.arena

import android.content.SharedPreferences
import kotlin.math.max

interface ArenaLifecycleSequenceStore {
    fun nextSequence(): Int
    fun advanceToAtLeast(
        sequence: Int
    )
}

class SharedPreferencesArenaLifecycleSequenceStore(
    private val preferences: SharedPreferences
) : ArenaLifecycleSequenceStore by PersistedArenaLifecycleSequenceStore(
    readSequence = {
        preferences.getInt(
            KEY_LIFECYCLE_SEQUENCE,
            0
        )
    },
    writeSequence = { sequence ->
        preferences.edit()
            .putInt(
                KEY_LIFECYCLE_SEQUENCE,
                sequence
            )
            .apply()
    }
) {
    companion object {
        private const val KEY_LIFECYCLE_SEQUENCE =
            "arena_lifecycle_event_sequence"
    }
}

class PersistedArenaLifecycleSequenceStore(
    private val readSequence: () -> Int,
    private val writeSequence: (Int) -> Unit
) : ArenaLifecycleSequenceStore {
    override fun nextSequence(): Int {
        synchronized(this) {
            val nextSequence =
                readSequence() + 1

            writeSequence(
                nextSequence
            )
            return nextSequence
        }
    }

    override fun advanceToAtLeast(
        sequence: Int
    ) {
        synchronized(this) {
            writeSequence(
                max(
                    readSequence(),
                    sequence.coerceAtLeast(
                        0
                    )
                )
            )
        }
    }
}
