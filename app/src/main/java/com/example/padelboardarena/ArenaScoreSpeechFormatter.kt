package com.example.padelboardarena

data class ArenaScoreSpeechState(

    val pointsA: Int,

    val pointsB: Int,

    val gamesA: Int,

    val gamesB: Int,

    val advantageSide: Side?,

    val tieBreakActive: Boolean = false

)

class ArenaScoreSpeechFormatter(

    private val teamALabel: String = "Squadra A",

    private val teamBLabel: String = "Squadra B"

) {

    fun formatPoint(

        state: ArenaScoreSpeechState,

        scoringSide: Side,

        previousGamesA: Int,

        previousGamesB: Int

    ): String {

        val gameWinner =

            when {

                state.gamesA > previousGamesA -> Side.A

                state.gamesB > previousGamesB -> Side.B

                else -> null

            }

        if (gameWinner != null) {

            return "Game ${teamLabel(gameWinner)}. ${gameScorePhrase(state)}"

        }

        return scorePhrase(

            state

        )

    }

    fun formatUndo(

        state: ArenaScoreSpeechState,

        previousGamesA: Int,

        previousGamesB: Int

    ): String {

        val phrases =

            mutableListOf(

                "Correzione"

            )

        if (

            state.gamesA != previousGamesA ||

            state.gamesB != previousGamesB

        ) {

            phrases.add(

                gameScorePhrase(

                    state

                )

            )

        }

        phrases.add(

            scorePhrase(

                state

            )

        )

        return phrases.joinToString(

            separator = ". "

        )

    }

    fun formatGameCorrection(

        state: ArenaScoreSpeechState,

        correctedSide: Side

    ): String {

        return "Correzione. Game ${teamLabel(correctedSide)}. ${gameScorePhrase(state)}"

    }

    private fun scorePhrase(

        state: ArenaScoreSpeechState

    ): String {

        val advantageSide =

            state.advantageSide

        if (state.tieBreakActive) {
            return "${numberWord(state.pointsA)} a ${numberWord(state.pointsB)}"
        }

        if (advantageSide != null) {

            return "Vantaggio ${teamLabel(advantageSide)}"

        }

        val pointA =
            pointWord(
                value = state.pointsA,
                sentenceStart = true
            )

        val pointB =
            pointWord(
                value = state.pointsB,
                sentenceStart = false
            )

        return if (state.pointsA == state.pointsB) {

            "$pointA pari"

        } else {

            "$pointA $pointB"

        }

    }

    private fun gameScorePhrase(

        state: ArenaScoreSpeechState

    ): String {

        return "${numberWord(state.gamesA)} a ${numberWord(state.gamesB)}"

    }

    private fun teamLabel(

        side: Side

    ): String {

        return when (side) {

            Side.A -> teamALabel

            Side.B -> teamBLabel

        }

    }

    private fun pointWord(
        value: Int,
        sentenceStart: Boolean
    ): String {
        return when (value) {
            0 -> "zero"
            1 -> if (sentenceStart) "Quindici" else "quindici"
            2 -> if (sentenceStart) "Trenta" else "trenta"
            3 -> if (sentenceStart) "Quaranta" else "quaranta"
            else -> value.toString()
        }
    }

    private fun numberWord(

        value: Int

    ): String {

        return when (value) {

            0 -> "zero"

            1 -> "Uno"

            2 -> "Due"

            3 -> "Tre"

            4 -> "Quattro"

            5 -> "Cinque"

            6 -> "Sei"

            7 -> "Sette"

            8 -> "Otto"

            9 -> "Nove"

            10 -> "Dieci"

            else -> value.toString()

        }

    }

}

