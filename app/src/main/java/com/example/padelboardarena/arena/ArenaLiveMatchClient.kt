package com.example.padelboardarena.arena

import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ArenaLiveMatchSide(
    val label: String,
    val teamId: String?
)

data class ArenaLiveMatch(
    val matchId: String,
    val sideA: ArenaLiveMatchSide,
    val sideB: ArenaLiveMatchSide,
    val score: String?,
    val scoreA: Int,
    val scoreB: Int,
    val setsA: Int,
    val setsB: Int,
    val lastLifecycleEventSequence: Int,
    val status: String?,
    val phase: String?,
    val scoreMode: String?
)

data class ArenaLiveMatchResponse(
    val courtId: String,
    val arenaCourtLabel: String?,
    val tournamentCourtName: String?,
    val match: ArenaLiveMatch?,
    val reason: String?
) {
    companion object {
        fun fromJson(
            body: String
        ): ArenaLiveMatchResponse {
            require(body.jsonBoolean("ok") == true) {
                "Live match response not ok"
            }

            val matchObject =
                body.jsonObject(
                    "match"
                )

            return ArenaLiveMatchResponse(
                courtId = body.jsonString("courtId").orEmpty(),
                arenaCourtLabel = body.jsonString("arenaCourtLabel"),
                tournamentCourtName = body.jsonString("tournamentCourtName"),
                match = matchObject?.let { match ->
                    ArenaLiveMatch(
                        matchId = match.jsonString("matchId").orEmpty(),
                        sideA = parseSide(
                            match.jsonObject("sideA")
                        ),
                        sideB = parseSide(
                            match.jsonObject("sideB")
                        ),
                        score = match.jsonString("score"),
                        scoreA = match.jsonNumber("scoreA")?.toInt() ?: 0,
                        scoreB = match.jsonNumber("scoreB")?.toInt() ?: 0,
                        setsA = match.jsonNumber("setsA")?.toInt() ?: 0,
                        setsB = match.jsonNumber("setsB")?.toInt() ?: 0,
                        lastLifecycleEventSequence =
                            match.jsonNumber("lastLifecycleEventSequence")?.toInt() ?: 0,
                        status = match.jsonString("status"),
                        phase = match.jsonString("phase"),
                        scoreMode = match.jsonString("scoreMode")
                    )
                },
                reason = body.jsonString("reason")
            )
        }

        private fun parseSide(
            input: String?
        ): ArenaLiveMatchSide {
            return ArenaLiveMatchSide(
                label = input?.jsonString("label").orEmpty(),
                teamId = input?.jsonString("teamId")
            )
        }
    }
}

data class ArenaLiveMatchResult(
    val statusCode: Int,
    val response: ArenaLiveMatchResponse?,
    val body: String
) {
    val success: Boolean =
        statusCode in 200..299 && response != null

    companion object {
        fun fromHttp(
            statusCode: Int,
            body: String
        ): ArenaLiveMatchResult {
            val parsedResponse =
                if (statusCode in 200..299) {
                    runCatching {
                        ArenaLiveMatchResponse.fromJson(
                            body
                        )
                    }.getOrNull()
                } else {
                    null
                }

            return ArenaLiveMatchResult(
                statusCode = statusCode,
                response = parsedResponse,
                body = body
            )
        }
    }
}

class ArenaLiveMatchClient(
    private val config: ArenaConfig,
    private val tokenProvider: ArenaTokenProvider,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport()
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    fun fetchLiveMatch(
        callback: (ArenaLiveMatchResult) -> Unit
    ) {
        executor.execute {
            try {
                callback(
                    fetchLiveMatchBlocking()
                )
            } catch (error: Exception) {
                callback(
                    ArenaLiveMatchResult.fromHttp(
                        statusCode = 0,
                        body = "network_error"
                    )
                )
            }
        }
    }

    fun fetchLiveMatchBlocking(): ArenaLiveMatchResult {
        config.requireComplete()

        val accessToken =
            tokenProvider.currentAccessToken()
                ?: return ArenaLiveMatchResult.fromHttp(
                    statusCode = 401,
                    body = "Missing access token"
                )

        val firstResult =
            runCatching {
                getLiveMatch(
                    accessToken = accessToken
                )
            }.getOrElse { error ->
                return networkErrorResult(
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
            getLiveMatch(
                accessToken = refreshedToken
            )
        }.getOrElse { error ->
            networkErrorResult(
                error
            )
        }
    }

    fun liveMatchUrl(): String {
        return "${config.apiBaseUrl.trimEnd('/')}" +
                "/api/arena/courts/" +
                config.courtId.trim('/') +
                "/live-match"
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun getLiveMatch(
        accessToken: String
    ): ArenaLiveMatchResult {
        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "GET",
                    url = liveMatchUrl(),
                    headers = mapOf(
                        "Authorization" to "Bearer $accessToken"
                    ),
                    body = ""
                )
            )

        return ArenaLiveMatchResult.fromHttp(
            statusCode = response.statusCode,
            body = response.body
        )
    }
}

private fun networkErrorResult(
    error: Throwable
): ArenaLiveMatchResult {
    if (error is IOException) {
        return ArenaLiveMatchResult.fromHttp(
            statusCode = 0,
            body = "network_error"
        )
    }

    return ArenaLiveMatchResult.fromHttp(
        statusCode = 0,
        body = "network_error"
    )
}

private fun String.jsonBoolean(
    name: String
): Boolean? {
    val pattern =
        Regex(
            "\"${Regex.escape(name)}\"\\s*:\\s*(true|false)"
        )

    return pattern.find(this)
        ?.groupValues
        ?.get(1)
        ?.toBooleanStrictOrNull()
}

private fun String.jsonObject(
    name: String
): String? {
    val marker =
        Regex(
            "\"${Regex.escape(name)}\"\\s*:\\s*\\{"
        ).find(this) ?: return null

    val objectStart =
        marker.range.last

    var depth = 1
    var index =
        objectStart + 1
    var inString = false
    var escaped = false

    while (index < length) {
        val char =
            this[index]

        if (escaped) {
            escaped = false
            index += 1
            continue
        }

        when {
            inString && char == '\\' ->
                escaped = true

            char == '"' ->
                inString = !inString

            !inString && char == '{' ->
                depth += 1

            !inString && char == '}' -> {
                depth -= 1

                if (depth == 0) {
                    return substring(
                        objectStart,
                        index + 1
                    )
                }
            }
        }

        index += 1
    }

    return null
}
