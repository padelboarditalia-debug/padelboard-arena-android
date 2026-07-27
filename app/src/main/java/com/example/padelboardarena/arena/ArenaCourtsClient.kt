package com.example.padelboardarena.arena

import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ArenaCourt(
    val courtId: String,
    val centerId: String,
    val centerName: String,
    val label: String,
    val tournamentCourtName: String?,
    val isActive: Boolean
)

data class ArenaCourtsResult(
    val statusCode: Int,
    val courts: List<ArenaCourt>,
    val body: String
) {
    val success: Boolean =
        statusCode in 200..299

    companion object {
        fun fromHttp(
            statusCode: Int,
            body: String
        ): ArenaCourtsResult {
            val parsedCourts =
                if (statusCode in 200..299) {
                    runCatching {
                        parseArenaCourts(
                            body
                        )
                    }.getOrElse {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

            return ArenaCourtsResult(
                statusCode = statusCode,
                courts = parsedCourts,
                body = body
            )
        }
    }
}

class ArenaCourtsClient(
    private val config: ArenaConfig,
    private val tokenProvider: ArenaTokenProvider,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport()
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    fun fetchCourts(
        callback: (ArenaCourtsResult) -> Unit
    ) {
        executor.execute {
            try {
                callback(
                    fetchCourtsBlocking()
                )
            } catch (error: Exception) {
                callback(
                    ArenaCourtsResult.fromHttp(
                        statusCode = 0,
                        body = "network_error"
                    )
                )
            }
        }
    }

    fun fetchCourtsBlocking(): ArenaCourtsResult {
        config.requireAuthComplete()
        require(config.apiBaseUrl.isNotBlank()) {
            "ARENA_API_BASE_URL missing"
        }

        val accessToken =
            tokenProvider.currentAccessToken()
                ?: return ArenaCourtsResult.fromHttp(
                    statusCode = 401,
                    body = "Missing access token"
                )

        val firstResult =
            runCatching {
                getCourts(
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
            getCourts(
                accessToken = refreshedToken
            )
        }.getOrElse { error ->
            networkErrorResult(
                error
            )
        }
    }

    fun courtsUrl(): String {
        return "${config.apiBaseUrl.trimEnd('/')}" +
                "/api/arena/courts"
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun getCourts(
        accessToken: String
    ): ArenaCourtsResult {
        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "GET",
                    url = courtsUrl(),
                    headers = mapOf(
                        "Authorization" to "Bearer $accessToken"
                    ),
                    body = ""
                )
            )

        return ArenaCourtsResult.fromHttp(
            statusCode = response.statusCode,
            body = response.body
        )
    }
}

private fun networkErrorResult(
    error: Throwable
): ArenaCourtsResult {
    if (error is IOException) {
        return ArenaCourtsResult.fromHttp(
            statusCode = 0,
            body = "network_error"
        )
    }

    return ArenaCourtsResult.fromHttp(
        statusCode = 0,
        body = "network_error"
    )
}

private fun parseArenaCourts(
    body: String
): List<ArenaCourt> {
    if (body.jsonBoolean("ok") != true) {
        return emptyList()
    }

    return body.jsonArrayObjects("courts").map { court ->
        ArenaCourt(
            courtId = court.jsonString("courtId").orEmpty(),
            centerId = court.jsonString("centerId").orEmpty(),
            centerName = court.jsonString("centerName").orEmpty(),
            label = court.jsonString("label").orEmpty(),
            tournamentCourtName = court.jsonString("tournamentCourtName"),
            isActive = court.jsonBoolean("isActive") == true
        )
    }.filter { court ->
        court.courtId.isNotBlank()
    }
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

private fun String.jsonArrayObjects(
    name: String
): List<String> {
    val marker =
        Regex(
            "\"${Regex.escape(name)}\"\\s*:\\s*\\["
        ).find(this) ?: return emptyList()

    val arrayStart =
        marker.range.last
    val arrayEnd =
        findJsonBlockEnd(
            start = arrayStart,
            opening = '[',
            closing = ']'
        ) ?: return emptyList()

    return substring(
        arrayStart + 1,
        arrayEnd
    ).scanJsonObjects()
}

private fun String.scanJsonObjects(): List<String> {
    val objects =
        mutableListOf<String>()
    var index = 0

    while (index < length) {
        if (this[index] != '{') {
            index += 1
            continue
        }

        val end =
            findJsonBlockEnd(
                start = index,
                opening = '{',
                closing = '}'
            ) ?: break

        objects.add(
            substring(
                index,
                end + 1
            )
        )
        index = end + 1
    }

    return objects
}

private fun String.findJsonBlockEnd(
    start: Int,
    opening: Char,
    closing: Char
): Int? {
    var depth = 1
    var index =
        start + 1
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

            !inString && char == opening ->
                depth += 1

            !inString && char == closing -> {
                depth -= 1

                if (depth == 0) {
                    return index
                }
            }
        }

        index += 1
    }

    return null
}
