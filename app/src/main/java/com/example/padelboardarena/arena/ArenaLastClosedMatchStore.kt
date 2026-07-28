package com.example.padelboardarena.arena

import android.content.Context
import android.content.SharedPreferences

data class ArenaLastClosedMatch(
    val matchId: String,
    val courtId: String,
    val teamLabelA: String,
    val teamLabelB: String,
    val pointsA: Int,
    val pointsB: Int,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int,
    val advantageSide: String?,
    val killerMode: Boolean,
    val tieBreakActive: Boolean,
    val tieBreakPointsA: Int,
    val tieBreakPointsB: Int,
    val finishEventId: String,
    val finishEventSequence: Int,
    val finishedAt: String,
    val reopenAvailable: Boolean
)

class ArenaLastClosedMatchStore(
    private val preferences: SharedPreferences
) {
    constructor(
        context: Context
    ) : this(
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    )

    fun read(): ArenaLastClosedMatch? {
        if (!preferences.getBoolean(KEY_REOPEN_AVAILABLE, false)) {
            return null
        }

        val matchId =
            preferences.getString(KEY_MATCH_ID, null)?.takeIf { it.isNotBlank() }
                ?: return null
        val courtId =
            preferences.getString(KEY_COURT_ID, null)?.takeIf { it.isNotBlank() }
                ?: return null

        return ArenaLastClosedMatch(
            matchId = matchId,
            courtId = courtId,
            teamLabelA = preferences.getString(KEY_TEAM_LABEL_A, null).orEmpty(),
            teamLabelB = preferences.getString(KEY_TEAM_LABEL_B, null).orEmpty(),
            pointsA = preferences.getInt(KEY_POINTS_A, 0),
            pointsB = preferences.getInt(KEY_POINTS_B, 0),
            gamesA = preferences.getInt(KEY_GAMES_A, 0),
            gamesB = preferences.getInt(KEY_GAMES_B, 0),
            setsA = preferences.getInt(KEY_SETS_A, 0),
            setsB = preferences.getInt(KEY_SETS_B, 0),
            advantageSide = preferences.getString(KEY_ADVANTAGE_SIDE, null),
            killerMode = preferences.getBoolean(KEY_KILLER_MODE, false),
            tieBreakActive = preferences.getBoolean(KEY_TIE_BREAK_ACTIVE, false),
            tieBreakPointsA = preferences.getInt(KEY_TIE_BREAK_POINTS_A, 0),
            tieBreakPointsB = preferences.getInt(KEY_TIE_BREAK_POINTS_B, 0),
            finishEventId = preferences.getString(KEY_FINISH_EVENT_ID, null).orEmpty(),
            finishEventSequence = preferences.getInt(KEY_FINISH_EVENT_SEQUENCE, 0),
            finishedAt = preferences.getString(KEY_FINISHED_AT, null).orEmpty(),
            reopenAvailable = true
        )
    }

    fun save(
        match: ArenaLastClosedMatch
    ) {
        preferences.edit()
            .putString(KEY_MATCH_ID, match.matchId)
            .putString(KEY_COURT_ID, match.courtId)
            .putString(KEY_TEAM_LABEL_A, match.teamLabelA)
            .putString(KEY_TEAM_LABEL_B, match.teamLabelB)
            .putInt(KEY_POINTS_A, match.pointsA)
            .putInt(KEY_POINTS_B, match.pointsB)
            .putInt(KEY_GAMES_A, match.gamesA)
            .putInt(KEY_GAMES_B, match.gamesB)
            .putInt(KEY_SETS_A, match.setsA)
            .putInt(KEY_SETS_B, match.setsB)
            .putString(KEY_ADVANTAGE_SIDE, match.advantageSide)
            .putBoolean(KEY_KILLER_MODE, match.killerMode)
            .putBoolean(KEY_TIE_BREAK_ACTIVE, match.tieBreakActive)
            .putInt(KEY_TIE_BREAK_POINTS_A, match.tieBreakPointsA)
            .putInt(KEY_TIE_BREAK_POINTS_B, match.tieBreakPointsB)
            .putString(KEY_FINISH_EVENT_ID, match.finishEventId)
            .putInt(KEY_FINISH_EVENT_SEQUENCE, match.finishEventSequence)
            .putString(KEY_FINISHED_AT, match.finishedAt)
            .putBoolean(KEY_REOPEN_AVAILABLE, match.reopenAvailable)
            .apply()
    }

    fun markReopenUnavailable() {
        preferences.edit()
            .putBoolean(KEY_REOPEN_AVAILABLE, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "padelboard_arena"
        private const val PREFIX = "last_closed_arena_match_"
        private const val KEY_MATCH_ID = "${PREFIX}match_id"
        private const val KEY_COURT_ID = "${PREFIX}court_id"
        private const val KEY_TEAM_LABEL_A = "${PREFIX}team_label_a"
        private const val KEY_TEAM_LABEL_B = "${PREFIX}team_label_b"
        private const val KEY_POINTS_A = "${PREFIX}points_a"
        private const val KEY_POINTS_B = "${PREFIX}points_b"
        private const val KEY_GAMES_A = "${PREFIX}games_a"
        private const val KEY_GAMES_B = "${PREFIX}games_b"
        private const val KEY_SETS_A = "${PREFIX}sets_a"
        private const val KEY_SETS_B = "${PREFIX}sets_b"
        private const val KEY_ADVANTAGE_SIDE = "${PREFIX}advantage_side"
        private const val KEY_KILLER_MODE = "${PREFIX}killer_mode"
        private const val KEY_TIE_BREAK_ACTIVE = "${PREFIX}tie_break_active"
        private const val KEY_TIE_BREAK_POINTS_A = "${PREFIX}tie_break_points_a"
        private const val KEY_TIE_BREAK_POINTS_B = "${PREFIX}tie_break_points_b"
        private const val KEY_FINISH_EVENT_ID = "${PREFIX}finish_event_id"
        private const val KEY_FINISH_EVENT_SEQUENCE = "${PREFIX}finish_event_sequence"
        private const val KEY_FINISHED_AT = "${PREFIX}finished_at"
        private const val KEY_REOPEN_AVAILABLE = "${PREFIX}reopen_available"
    }
}
