package com.example.padelboardarena

import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue

import org.junit.Test

class ArenaScoreSpeechFormatterTest {

    private val formatter =

        ArenaScoreSpeechFormatter()

    @Test

    fun fifteenZeroProducesItalianScore() {

        val phrase =

            formatter.formatPoint(

                state = ArenaScoreSpeechState(

                    pointsA = 1,

                    pointsB = 0,

                    gamesA = 0,

                    gamesB = 0,

                    advantageSide = null

                ),

                scoringSide = Side.A,

                previousGamesA = 0,

                previousGamesB = 0

            )

        assertEquals(

            "Quindici zero",

            phrase

        )

    }

    @Test

    fun fortyAllProducesItalianParity() {

        val phrase =

            formatter.formatPoint(

                state = ArenaScoreSpeechState(

                    pointsA = 3,

                    pointsB = 3,

                    gamesA = 0,

                    gamesB = 0,

                    advantageSide = null

                ),

                scoringSide = Side.A,

                previousGamesA = 0,

                previousGamesB = 0

            )

        assertEquals(

            "Quaranta pari",

            phrase

        )

    }

    @Test

    fun advantageAProducesTeamLabel() {

        val phrase =

            formatter.formatPoint(

                state = ArenaScoreSpeechState(

                    pointsA = 3,

                    pointsB = 3,

                    gamesA = 0,

                    gamesB = 0,

                    advantageSide = Side.A

                ),

                scoringSide = Side.A,

                previousGamesA = 0,

                previousGamesB = 0

            )

        assertEquals(

            "Vantaggio Squadra A",

            phrase

        )

    }

    @Test

    fun gameAOneZeroProducesGameAndGameScore() {

        val phrase =

            formatter.formatPoint(

                state = ArenaScoreSpeechState(

                    pointsA = 0,

                    pointsB = 0,

                    gamesA = 1,

                    gamesB = 0,

                    advantageSide = null

                ),

                scoringSide = Side.A,

                previousGamesA = 0,

                previousGamesB = 0

            )

        assertTrue(

            phrase.contains(

                "Game Squadra A"

            )

        )

        assertTrue(

            phrase.contains(

                "Uno a zero"

            )

        )

    }

    @Test

    fun undoStartsWithCorrectionAndRestoredScore() {

        val phrase =

            formatter.formatUndo(

                state = ArenaScoreSpeechState(

                    pointsA = 1,

                    pointsB = 2,

                    gamesA = 0,

                    gamesB = 0,

                    advantageSide = null

                ),

                previousGamesA = 0,

                previousGamesB = 0

            )

        assertEquals(

            "Correzione. Quindici trenta",

            phrase

        )

    }

    @Test

    fun undoRestoringPreviousGameAlsoSpeaksGameScore() {

        val phrase =

            formatter.formatUndo(

                state = ArenaScoreSpeechState(

                    pointsA = 0,

                    pointsB = 0,

                    gamesA = 1,

                    gamesB = 0,

                    advantageSide = null

                ),

                previousGamesA = 2,

                previousGamesB = 0

            )

        assertEquals(

            "Correzione. Uno a zero. zero pari",

            phrase

        )

    }

    @Test

    fun formatterDoesNotExposeTechnicalData() {

        val source =

            java.io.File(

                "src/main/java/com/example/padelboardarena/ArenaScoreSpeechFormatter.kt"

            ).readText()

        assertTrue(

            !source.contains(

                "password"

            )

        )

        assertTrue(

            !source.contains(

                "token"

            )

        )

        assertTrue(

            !source.contains(

                "UUID"

            )

        )

    }

}

