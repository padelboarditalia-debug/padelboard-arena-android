package com.example.padelboardarena

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaClassicScoringTest {
    @Test
    fun sixFourWinsSetAndResetsGames() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 5,
                    gamesB = 4,
                    pointsA = 3
                ),
                scoringSide = Side.A
            )

        assertEquals(Side.A, result.setWinner)
        assertEquals(1, result.state.setsA)
        assertEquals(0, result.state.gamesA)
        assertEquals(0, result.state.gamesB)
        assertFalse(result.state.localMatchFinished)
    }

    @Test
    fun sixFiveDoesNotWinSet() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 5,
                    gamesB = 5,
                    pointsA = 3
                ),
                scoringSide = Side.A
            )

        assertNull(result.setWinner)
        assertEquals(6, result.state.gamesA)
        assertEquals(5, result.state.gamesB)
        assertEquals(0, result.state.setsA)
    }

    @Test
    fun sevenFiveWinsSet() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 6,
                    gamesB = 5,
                    pointsA = 3
                ),
                scoringSide = Side.A
            )

        assertEquals(Side.A, result.setWinner)
        assertEquals(1, result.state.setsA)
        assertEquals(0, result.state.gamesA)
        assertEquals(0, result.state.gamesB)
    }

    @Test
    fun sixAllStartsTieBreak() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 5,
                    gamesB = 6,
                    pointsA = 3
                ),
                scoringSide = Side.A
            )

        assertNull(result.setWinner)
        assertTrue(result.state.tieBreakActive)
        assertEquals(6, result.state.gamesA)
        assertEquals(6, result.state.gamesB)
        assertEquals(0, result.state.tieBreakPointsA)
        assertEquals(0, result.state.tieBreakPointsB)
    }

    @Test
    fun tieBreakSevenFiveWinsSet() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 6,
                    gamesB = 6,
                    tieBreakActive = true,
                    tieBreakPointsA = 6,
                    tieBreakPointsB = 5
                ),
                scoringSide = Side.A
            )

        assertEquals(Side.A, result.setWinner)
        assertFalse(result.state.tieBreakActive)
        assertEquals(1, result.state.setsA)
    }

    @Test
    fun tieBreakSevenSixContinues() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 6,
                    gamesB = 6,
                    tieBreakActive = true,
                    tieBreakPointsA = 6,
                    tieBreakPointsB = 6
                ),
                scoringSide = Side.A
            )

        assertNull(result.setWinner)
        assertTrue(result.state.tieBreakActive)
        assertEquals(7, result.state.tieBreakPointsA)
        assertEquals(6, result.state.tieBreakPointsB)
    }

    @Test
    fun tieBreakEightSixWinsSet() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    gamesA = 6,
                    gamesB = 6,
                    tieBreakActive = true,
                    tieBreakPointsA = 7,
                    tieBreakPointsB = 6
                ),
                scoringSide = Side.A
            )

        assertEquals(Side.A, result.setWinner)
        assertEquals(1, result.state.setsA)
        assertFalse(result.state.tieBreakActive)
    }

    @Test
    fun secondSetWinsLocalMatch() {
        val result =
            ArenaClassicScoring.registerPoint(
                state = ArenaClassicScoring.reset().copy(
                    setsA = 1,
                    gamesA = 5,
                    pointsA = 3
                ),
                scoringSide = Side.A
            )

        assertEquals(Side.A, result.setWinner)
        assertEquals(Side.A, result.matchWinner)
        assertEquals(2, result.state.setsA)
        assertTrue(result.state.localMatchFinished)
    }

    @Test
    fun finishedMatchIgnoresNewPoints() {
        val state =
            ArenaClassicScoring.reset().copy(
                setsA = 2,
                localMatchFinished = true
            )

        val result =
            ArenaClassicScoring.registerPoint(
                state = state,
                scoringSide = Side.B
            )

        assertEquals(state, result.state)
        assertNull(result.setWinner)
        assertNull(result.matchWinner)
    }

    @Test
    fun resetClearsEveryLocalScoreField() {
        val state =
            ArenaClassicScoring.reset()

        assertEquals(0, state.pointsA)
        assertEquals(0, state.pointsB)
        assertEquals(0, state.gamesA)
        assertEquals(0, state.gamesB)
        assertEquals(0, state.setsA)
        assertEquals(0, state.setsB)
        assertFalse(state.tieBreakActive)
        assertEquals(0, state.tieBreakPointsA)
        assertEquals(0, state.tieBreakPointsB)
        assertFalse(state.localMatchFinished)
    }
}
