package com.example.padelboardarena.arena

object ArenaManualUiMessages {
    fun loginFailed(
        error: Throwable
    ): String {
        return "Login Arena richiesto"
    }

    fun apiResult(
        result: ArenaApiResult
    ): String {
        return when (result.statusCode) {
            in 200..299 ->
                "Arena aggiornata"

            401 ->
                "Login Arena richiesto"

            else ->
                "Invio Arena fallito"
        }
    }

    fun apiDiagnostic(
        label: String,
        snapshot: ArenaScoreSnapshot,
        result: ArenaApiResult
    ): String {
        return "$label: HTTP ${result.statusCode}\n" +
                "body: ${result.body}\n" +
                "eventId: ${snapshot.eventId}\n" +
                "eventSequence: ${snapshot.eventSequence}\n" +
                "pointsA: ${snapshot.sideA.points}, " +
                "pointsB: ${snapshot.sideB.points}\n" +
                "gamesA: ${snapshot.sideA.games}, " +
                "gamesB: ${snapshot.sideB.games}"
    }

    fun apiFailed(
        error: Throwable
    ): String {
        return "Invio Arena fallito"
    }
}
