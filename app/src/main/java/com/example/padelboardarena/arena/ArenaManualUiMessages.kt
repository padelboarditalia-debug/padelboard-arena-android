package com.example.padelboardarena.arena

object ArenaManualUiMessages {
    fun loginFailed(
        error: Throwable
    ): String {
        val message =
            error.message.orEmpty()

        return when {
            message.contains("HTTP 400") ||
                    message.contains("HTTP 401") ->
                "Login Arena non riuscito: credenziali non valide"

            message.contains("ARENA_") ||
                    message.contains("SUPABASE_") ->
                "Login Arena non riuscito: configurazione incompleta"

            else ->
                "Login Arena non riuscito: rete non disponibile"
        }
    }

    fun apiResult(
        result: ArenaApiResult
    ): String {
        return when (result.statusCode) {
            in 200..299 ->
                "Invio Arena riuscito"

            400 ->
                "Invio Arena non valido: 400"

            401 ->
                "Invio Arena non autorizzato: 401"

            403 ->
                "Invio Arena vietato: 403"

            404 ->
                "Campo Arena non trovato: 404"

            409 ->
                "Invio Arena in conflitto: 409"

            in 500..599 ->
                "Errore server Arena: ${result.statusCode}"

            else ->
                "Invio Arena fallito: ${result.statusCode}"
        }
    }

    fun apiFailed(
        error: Throwable
    ): String {
        val message =
            error.message.orEmpty()

        return if (
            message.contains("ARENA_") ||
            message.contains("SUPABASE_")
        ) {
            "Invio Arena fallito: configurazione incompleta"
        } else {
            "Invio Arena fallito: rete non disponibile"
        }
    }
}
