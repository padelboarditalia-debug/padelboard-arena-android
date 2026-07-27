package com.example.padelboardarena

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class ArenaScoreAnnouncer(
    context: Context,
    private var formatter: ArenaScoreSpeechFormatter =
        ArenaScoreSpeechFormatter()
) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? =
        TextToSpeech(
            context.applicationContext,
            this
        )

    private var ready = false

    override fun onInit(
        status: Int
    ) {
        val tts =
            textToSpeech ?: return

        if (status != TextToSpeech.SUCCESS) {
            shutdown()
            return
        }

        val languageResult =
            tts.setLanguage(
                Locale.ITALIAN
            )

        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            shutdown()
            return
        }

        tts.setSpeechRate(
            0.85f
        )

        tts.setPitch(
            1.0f
        )

        ready = true
    }

    fun announcePoint(
        state: ArenaScoreSpeechState,
        scoringSide: Side,
        previousGamesA: Int,
        previousGamesB: Int
    ) {
        speak(
            formatter.formatPoint(
                state = state,
                scoringSide = scoringSide,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
        )
    }

    fun announceUndo(
        state: ArenaScoreSpeechState,
        previousGamesA: Int,
        previousGamesB: Int
    ) {
        speak(
            formatter.formatUndo(
                state = state,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
        )
    }

    fun updateTeamLabels(
        teamALabel: String,
        teamBLabel: String
    ) {
        formatter =
            ArenaScoreSpeechFormatter(
                teamALabel = teamALabel,
                teamBLabel = teamBLabel
            )
    }

    private fun speak(
        phrase: String
    ) {
        if (!ready) {
            return
        }

        textToSpeech?.speak(
            phrase,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "arena_score_${System.nanoTime()}"
        )
    }

    fun shutdown() {
        ready = false
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
