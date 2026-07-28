package com.example.padelboardarena.arena

import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ArenaLiveScoreAdoptionRequest(
    val eventId: String,
    val eventSequence: Int,
    val matchId: String,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int
) {
    fun toJson(): String {
        return buildString {
            append("{")
            appendJsonString("eventId", eventId)
            append(",")
            appendJsonNumber("eventSequence", eventSequence)
            append(",")
            appendJsonString("matchId", matchId)
            append(",")
            appendJsonNumber("gamesA", gamesA)
            append(",")
            appendJsonNumber("gamesB", gamesB)
            append(",")
            appendJsonNumber("setsA", setsA)
            append(",")
            appendJsonNumber("setsB", setsB)
            append("}")
        }
    }
}

data class ArenaLiveScoreAdoptionResult(
    val statusCode: Int,
    val status: String?,
    val body: String
) {
    val success: Boolean =
        statusCode in 200..299

    companion object {
        fun fromHttp(
            statusCode: Int,
            body: String
        ): ArenaLiveScoreAdoptionResult {
            return ArenaLiveScoreAdoptionResult(
                statusCode = statusCode,
                status = body.jsonString("status"),
                body = body
            )
        }
    }
}

class ArenaLiveScoreAdoptionClient(
    private val config: ArenaConfig,
    private val tokenProvider: ArenaTokenProvider,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport()
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    fun adoptLiveScore(
        request: ArenaLiveScoreAdoptionRequest,
        callback: (ArenaLiveScoreAdoptionResult) -> Unit
    ) {
        executor.execute {
            callback(
                adoptLiveScoreBlocking(
                    request
                )
            )
        }
    }

    fun adoptLiveScoreBlocking(
        request: ArenaLiveScoreAdoptionRequest
    ): ArenaLiveScoreAdoptionResult {
        config.requireComplete()

        val accessToken =
            tokenProvider.currentAccessToken()
                ?: return ArenaLiveScoreAdoptionResult.fromHttp(
                    statusCode = 401,
                    body = "Missing access token"
                )

        val firstResult =
            runCatching {
                postAdoptLiveScore(
                    body = request.toJson(),
                    accessToken = accessToken
                )
            }.getOrElse { error ->
                return adoptionNetworkErrorResult(
                    error
                )
            }

        if (firstResult.statusCode != 401) {
            return firstResult
        }

        val refreshedToken =
            tokenProvider.refreshAccessToken()
                ?: return firstResult

        return runCatching {
            postAdoptLiveScore(
                body = request.toJson(),
                accessToken = refreshedToken
            )
        }.getOrElse { error ->
            adoptionNetworkErrorResult(
                error
            )
        }
    }

    fun adoptLiveScoreUrl(): String {
        return "${config.apiBaseUrl.trimEnd('/')}" +
                "/api/arena/courts/" +
                config.courtId.trim('/') +
                "/adopt-live-score"
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun postAdoptLiveScore(
        body: String,
        accessToken: String
    ): ArenaLiveScoreAdoptionResult {
        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "POST",
                    url = adoptLiveScoreUrl(),
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to "Bearer $accessToken"
                    ),
                    body = body
                )
            )

        return ArenaLiveScoreAdoptionResult.fromHttp(
            statusCode = response.statusCode,
            body = response.body
        )
    }
}

private fun adoptionNetworkErrorResult(
    error: Throwable
): ArenaLiveScoreAdoptionResult {
    if (error is IOException) {
        return ArenaLiveScoreAdoptionResult.fromHttp(
            statusCode = 0,
            body = "network_error"
        )
    }

    return ArenaLiveScoreAdoptionResult.fromHttp(
        statusCode = 0,
        body = "network_error"
    )
}
