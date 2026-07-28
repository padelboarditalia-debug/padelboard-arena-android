package com.example.padelboardarena.arena

import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ArenaMatchLifecycleScoreSnapshot(
    val eventId: String,
    val eventSequence: Int,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int
) {
    fun finishJson(): String {
        return buildString {
            append("{")
            appendJsonString("eventId", eventId)
            append(",")
            appendJsonNumber("eventSequence", eventSequence)
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

data class ArenaMatchReopenRequest(
    val eventId: String,
    val eventSequence: Int,
    val matchId: String
) {
    fun toJson(): String {
        return buildString {
            append("{")
            appendJsonString("eventId", eventId)
            append(",")
            appendJsonNumber("eventSequence", eventSequence)
            append(",")
            appendJsonString("matchId", matchId)
            append("}")
        }
    }
}

data class ArenaLifecycleMatch(
    val matchId: String,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int,
    val score: String,
    val matchStatus: String
) {
    companion object {
        fun fromJson(
            body: String
        ): ArenaLifecycleMatch? {
            val match =
                body.jsonObject(
                    "match"
                ) ?: return null

            return ArenaLifecycleMatch(
                matchId = match.jsonString("matchId").orEmpty(),
                gamesA = match.jsonNumber("gamesA")?.toInt() ?: 0,
                gamesB = match.jsonNumber("gamesB")?.toInt() ?: 0,
                setsA = match.jsonNumber("setsA")?.toInt() ?: 0,
                setsB = match.jsonNumber("setsB")?.toInt() ?: 0,
                score = match.jsonString("score").orEmpty(),
                matchStatus = match.jsonString("matchStatus").orEmpty()
            )
        }
    }
}

data class ArenaMatchLifecycleResult(
    val statusCode: Int,
    val status: String?,
    val match: ArenaLifecycleMatch?,
    val body: String
) {
    val success: Boolean =
        statusCode in 200..299

    companion object {
        fun fromHttp(
            statusCode: Int,
            body: String
        ): ArenaMatchLifecycleResult {
            return ArenaMatchLifecycleResult(
                statusCode = statusCode,
                status = body.jsonString("status"),
                match =
                    if (statusCode in 200..299) {
                        runCatching {
                            ArenaLifecycleMatch.fromJson(
                                body
                            )
                        }.getOrNull()
                    } else {
                        null
                    },
                body = body
            )
        }
    }
}

class ArenaMatchLifecycleClient(
    private val config: ArenaConfig,
    private val tokenProvider: ArenaTokenProvider,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport()
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    fun finishMatch(
        snapshot: ArenaMatchLifecycleScoreSnapshot,
        callback: (ArenaMatchLifecycleResult) -> Unit
    ) {
        executor.execute {
            callback(
                finishMatchBlocking(
                    snapshot
                )
            )
        }
    }

    fun reopenMatch(
        request: ArenaMatchReopenRequest,
        callback: (ArenaMatchLifecycleResult) -> Unit
    ) {
        executor.execute {
            callback(
                reopenMatchBlocking(
                    request
                )
            )
        }
    }

    fun finishMatchBlocking(
        snapshot: ArenaMatchLifecycleScoreSnapshot
    ): ArenaMatchLifecycleResult {
        return postWithRefresh(
            path = "finish-match",
            body = snapshot.finishJson()
        )
    }

    fun reopenMatchBlocking(
        request: ArenaMatchReopenRequest
    ): ArenaMatchLifecycleResult {
        return postWithRefresh(
            path = "reopen-match",
            body = request.toJson()
        )
    }

    fun finishMatchUrl(): String {
        return lifecycleUrl(
            "finish-match"
        )
    }

    fun reopenMatchUrl(): String {
        return lifecycleUrl(
            "reopen-match"
        )
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun postWithRefresh(
        path: String,
        body: String
    ): ArenaMatchLifecycleResult {
        config.requireComplete()

        val accessToken =
            tokenProvider.currentAccessToken()
                ?: return ArenaMatchLifecycleResult.fromHttp(
                    statusCode = 401,
                    body = "Missing access token"
                )

        val firstResult =
            runCatching {
                postLifecycle(
                    path = path,
                    body = body,
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
            postLifecycle(
                path = path,
                body = body,
                accessToken = refreshedToken
            )
        }.getOrElse { error ->
            networkErrorResult(
                error
            )
        }
    }

    private fun postLifecycle(
        path: String,
        body: String,
        accessToken: String
    ): ArenaMatchLifecycleResult {
        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "POST",
                    url = lifecycleUrl(
                        path
                    ),
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to "Bearer $accessToken"
                    ),
                    body = body
                )
            )

        return ArenaMatchLifecycleResult.fromHttp(
            statusCode = response.statusCode,
            body = response.body
        )
    }

    private fun lifecycleUrl(
        path: String
    ): String {
        return "${config.apiBaseUrl.trimEnd('/')}" +
                "/api/arena/courts/" +
                config.courtId.trim('/') +
                "/$path"
    }
}

private fun networkErrorResult(
    error: Throwable
): ArenaMatchLifecycleResult {
    if (error is IOException) {
        return ArenaMatchLifecycleResult.fromHttp(
            statusCode = 0,
            body = "network_error"
        )
    }

    return ArenaMatchLifecycleResult.fromHttp(
        statusCode = 0,
        body = "network_error"
    )
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
