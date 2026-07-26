package com.example.padelboardarena.arena

import android.content.SharedPreferences

interface ArenaManualSequenceStore {
    fun nextSequence(): Int
}

class SharedPreferencesArenaManualSequenceStore(
    private val preferences: SharedPreferences
) : ArenaManualSequenceStore by PersistedArenaManualSequenceStore(
    readSequence = {
        preferences.getInt(
            KEY_MANUAL_SEQUENCE,
            0
        )
    },
    writeSequence = { sequence ->
        preferences.edit()
            .putInt(
                KEY_MANUAL_SEQUENCE,
                sequence
            )
            .apply()
    }
) {
    companion object {
        private const val KEY_MANUAL_SEQUENCE =
            "arena_manual_event_sequence"
    }
}

class PersistedArenaManualSequenceStore(
    private val readSequence: () -> Int,
    private val writeSequence: (Int) -> Unit
) : ArenaManualSequenceStore {
    override fun nextSequence(): Int {
        synchronized(this) {
            val nextSequence =
                readSequence() + 1

            writeSequence(nextSequence)
            return nextSequence
        }
    }
}
