package com.example.padelboardarena.arena

import android.content.Context
import android.content.SharedPreferences

data class ArenaCourtSelection(
    val selectedCourtId: String,
    val selectedCourtLabel: String,
    val selectedCenterName: String,
    val selectedTournamentCourtName: String?
)

class ArenaCourtSelectionStore(
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

    fun readSelection(): ArenaCourtSelection? {
        val courtId =
            preferences.getString(
                KEY_SELECTED_COURT_ID,
                null
            )?.trim().orEmpty()

        if (courtId.isBlank()) {
            return null
        }

        return ArenaCourtSelection(
            selectedCourtId = courtId,
            selectedCourtLabel =
                preferences.getString(
                    KEY_SELECTED_COURT_LABEL,
                    null
                )?.trim().orEmpty().ifBlank {
                    "Campo Arena"
                },
            selectedCenterName =
                preferences.getString(
                    KEY_SELECTED_CENTER_NAME,
                    null
                )?.trim().orEmpty().ifBlank {
                    "Centro"
                },
            selectedTournamentCourtName =
                preferences.getString(
                    KEY_SELECTED_TOURNAMENT_COURT_NAME,
                    null
                )?.trim()?.ifBlank {
                    null
                }
        )
    }

    fun saveSelection(
        court: ArenaCourt
    ) {
        preferences.edit()
            .putString(
                KEY_SELECTED_COURT_ID,
                court.courtId
            )
            .putString(
                KEY_SELECTED_COURT_LABEL,
                court.label
            )
            .putString(
                KEY_SELECTED_CENTER_NAME,
                court.centerName
            )
            .putString(
                KEY_SELECTED_TOURNAMENT_COURT_NAME,
                court.tournamentCourtName
            )
            .apply()
    }

    fun clearSelection() {
        preferences.edit()
            .remove(
                KEY_SELECTED_COURT_ID
            )
            .remove(
                KEY_SELECTED_COURT_LABEL
            )
            .remove(
                KEY_SELECTED_CENTER_NAME
            )
            .remove(
                KEY_SELECTED_TOURNAMENT_COURT_NAME
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "padelboard_arena"
        private const val KEY_SELECTED_COURT_ID =
            "arena_selected_court_id"
        private const val KEY_SELECTED_COURT_LABEL =
            "arena_selected_court_label"
        private const val KEY_SELECTED_CENTER_NAME =
            "arena_selected_center_name"
        private const val KEY_SELECTED_TOURNAMENT_COURT_NAME =
            "arena_selected_tournament_court_name"
    }
}
