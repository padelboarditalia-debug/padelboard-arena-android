package com.example.padelboardarena

data class ArenaClassicScoreState(
    val pointsA: Int,
    val pointsB: Int,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int,
    val advantageSide: Side?,
    val killerMode: Boolean,
    val tieBreakActive: Boolean,
    val tieBreakPointsA: Int,
    val tieBreakPointsB: Int,
    val localMatchFinished: Boolean
)

data class ArenaClassicPointResult(
    val state: ArenaClassicScoreState,
    val setWinner: Side?,
    val matchWinner: Side?
)

object ArenaClassicScoring {
    fun registerPoint(
        state: ArenaClassicScoreState,
        scoringSide: Side
    ): ArenaClassicPointResult {
        if (state.localMatchFinished) {
            return ArenaClassicPointResult(
                state = state,
                setWinner = null,
                matchWinner = null
            )
        }

        if (state.tieBreakActive) {
            return registerTieBreakPoint(
                state,
                scoringSide
            )
        }

        if (state.killerMode) {
            return winGame(
                state,
                scoringSide
            )
        }

        val currentAdvantage =
            state.advantageSide

        if (currentAdvantage != null) {
            return if (currentAdvantage == scoringSide) {
                winGame(
                    state,
                    scoringSide
                )
            } else {
                ArenaClassicPointResult(
                    state = state.copy(
                        pointsA = 3,
                        pointsB = 3,
                        advantageSide = null,
                        killerMode = true
                    ),
                    setWinner = null,
                    matchWinner = null
                )
            }
        }

        val scorerPoints =
            if (scoringSide == Side.A) state.pointsA else state.pointsB
        val opponentPoints =
            if (scoringSide == Side.A) state.pointsB else state.pointsA

        if (scorerPoints == 3 && opponentPoints == 3) {
            return ArenaClassicPointResult(
                state = state.copy(
                    advantageSide = scoringSide
                ),
                setWinner = null,
                matchWinner = null
            )
        }

        if (scorerPoints == 3 && opponentPoints < 3) {
            return winGame(
                state,
                scoringSide
            )
        }

        return ArenaClassicPointResult(
            state =
                if (scoringSide == Side.A) {
                    state.copy(pointsA = state.pointsA + 1)
                } else {
                    state.copy(pointsB = state.pointsB + 1)
                },
            setWinner = null,
            matchWinner = null
        )
    }

    fun reset(): ArenaClassicScoreState {
        return ArenaClassicScoreState(
            pointsA = 0,
            pointsB = 0,
            gamesA = 0,
            gamesB = 0,
            setsA = 0,
            setsB = 0,
            advantageSide = null,
            killerMode = false,
            tieBreakActive = false,
            tieBreakPointsA = 0,
            tieBreakPointsB = 0,
            localMatchFinished = false
        )
    }

    private fun registerTieBreakPoint(
        state: ArenaClassicScoreState,
        scoringSide: Side
    ): ArenaClassicPointResult {
        val nextState =
            if (scoringSide == Side.A) {
                state.copy(
                    tieBreakPointsA = state.tieBreakPointsA + 1
                )
            } else {
                state.copy(
                    tieBreakPointsB = state.tieBreakPointsB + 1
                )
            }

        val sidePoints =
            if (scoringSide == Side.A) nextState.tieBreakPointsA else nextState.tieBreakPointsB
        val opponentPoints =
            if (scoringSide == Side.A) nextState.tieBreakPointsB else nextState.tieBreakPointsA

        if (sidePoints >= 7 && sidePoints - opponentPoints >= 2) {
            return winSet(
                nextState,
                scoringSide
            )
        }

        return ArenaClassicPointResult(
            state = nextState,
            setWinner = null,
            matchWinner = null
        )
    }

    private fun winGame(
        state: ArenaClassicScoreState,
        winningSide: Side
    ): ArenaClassicPointResult {
        val nextGamesA =
            state.gamesA + if (winningSide == Side.A) 1 else 0
        val nextGamesB =
            state.gamesB + if (winningSide == Side.B) 1 else 0
        val afterGame =
            state.copy(
                pointsA = 0,
                pointsB = 0,
                gamesA = nextGamesA,
                gamesB = nextGamesB,
                advantageSide = null,
                killerMode = false
            )

        if (nextGamesA == 6 && nextGamesB == 6) {
            return ArenaClassicPointResult(
                state = afterGame.copy(
                    tieBreakActive = true,
                    tieBreakPointsA = 0,
                    tieBreakPointsB = 0
                ),
                setWinner = null,
                matchWinner = null
            )
        }

        val winningGames =
            if (winningSide == Side.A) nextGamesA else nextGamesB
        val opponentGames =
            if (winningSide == Side.A) nextGamesB else nextGamesA

        if (winningGames >= 6 && winningGames - opponentGames >= 2) {
            return winSet(
                afterGame,
                winningSide
            )
        }

        return ArenaClassicPointResult(
            state = afterGame,
            setWinner = null,
            matchWinner = null
        )
    }

    private fun winSet(
        state: ArenaClassicScoreState,
        winningSide: Side
    ): ArenaClassicPointResult {
        val nextSetsA =
            state.setsA + if (winningSide == Side.A) 1 else 0
        val nextSetsB =
            state.setsB + if (winningSide == Side.B) 1 else 0
        val matchWinner =
            when {
                nextSetsA >= 2 -> Side.A
                nextSetsB >= 2 -> Side.B
                else -> null
            }

        return ArenaClassicPointResult(
            state = state.copy(
                pointsA = 0,
                pointsB = 0,
                gamesA = 0,
                gamesB = 0,
                setsA = nextSetsA,
                setsB = nextSetsB,
                advantageSide = null,
                killerMode = false,
                tieBreakActive = false,
                tieBreakPointsA = 0,
                tieBreakPointsB = 0,
                localMatchFinished = matchWinner != null
            ),
            setWinner = winningSide,
            matchWinner = matchWinner
        )
    }
}
