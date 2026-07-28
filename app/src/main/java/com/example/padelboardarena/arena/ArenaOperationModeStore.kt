package com.example.padelboardarena.arena

import android.content.Context
import android.content.SharedPreferences

class ArenaOperationModeStore(
    private val preferences: SharedPreferences
) {
    constructor(
        context: Context
    ) : this(
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    )

    fun isStandaloneClassicMode(): Boolean {
        return preferences.getBoolean(
            KEY_STANDALONE_CLASSIC_MODE,
            false
        )
    }

    fun setStandaloneClassicMode(
        enabled: Boolean
    ) {
        preferences.edit()
            .putBoolean(
                KEY_STANDALONE_CLASSIC_MODE,
                enabled
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "padelboard_arena"
        private const val KEY_STANDALONE_CLASSIC_MODE =
            "arena_standalone_classic_mode"
    }
}
