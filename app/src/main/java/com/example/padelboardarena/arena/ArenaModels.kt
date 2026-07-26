package com.example.padelboardarena.arena

data class ArenaConfig(
    val apiBaseUrl: String,
    val courtId: String,
    val supabaseUrl: String,
    val supabasePublishableKey: String,
    val email: String
) {
    fun requireComplete() {
        require(apiBaseUrl.isNotBlank()) {
            "ARENA_API_BASE_URL missing"
        }
        require(courtId.isNotBlank()) {
            "ARENA_COURT_ID missing"
        }
        require(supabaseUrl.isNotBlank()) {
            "SUPABASE_URL missing"
        }
        require(supabasePublishableKey.isNotBlank()) {
            "SUPABASE_PUBLISHABLE_KEY missing"
        }
        require(email.isNotBlank()) {
            "ARENA_EMAIL missing"
        }
    }
}

data class ArenaScoreSnapshot(
    val eventId: String,
    val eventSequence: Int,
    val occurredAt: String,
    val matchStatus: String,
    val phase: String,
    val sideA: ArenaScoreSide,
    val sideB: ArenaScoreSide
) {
    fun toJson(): String {
        return buildString {
            append("{")
            appendJsonString("eventId", eventId)
            append(",")
            appendJsonNumber("eventSequence", eventSequence)
            append(",")
            appendJsonString("occurredAt", occurredAt)
            append(",")
            appendJsonString("matchStatus", matchStatus)
            append(",")
            appendJsonString("phase", phase)
            append(",")
            append("\"sideA\":")
            append(sideA.toJson())
            append(",")
            append("\"sideB\":")
            append(sideB.toJson())
            append("}")
        }
    }
}

data class ArenaScoreSide(
    val label: String,
    val points: String,
    val games: Int,
    val sets: Int
) {
    fun toJson(): String {
        return buildString {
            append("{")
            appendJsonString("label", label)
            append(",")
            appendJsonString("points", points)
            append(",")
            appendJsonNumber("games", games)
            append(",")
            appendJsonNumber("sets", sets)
            append("}")
        }
    }
}

data class ArenaAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val receivedAtMillis: Long
) {
    companion object {
        fun fromJson(
            body: String,
            receivedAtMillis: Long
        ): ArenaAuthSession {
            return ArenaAuthSession(
                accessToken = body.requireJsonString(
                    "access_token"
                ),
                refreshToken = body.requireJsonString(
                    "refresh_token"
                ),
                expiresIn = body.requireJsonLong(
                    "expires_in"
                ),
                receivedAtMillis = receivedAtMillis
            )
        }
    }
}

data class ArenaApiResult(
    val statusCode: Int,
    val retryable: Boolean,
    val body: String
) {
    val success: Boolean =
        statusCode in 200..299

    companion object {
        fun fromHttp(
            statusCode: Int,
            body: String
        ): ArenaApiResult {
            return ArenaApiResult(
                statusCode = statusCode,
                retryable = statusCode >= 500,
                body = body
            )
        }
    }
}

internal fun String.requireJsonString(
    name: String
): String {
    return jsonString(name)
        ?: throw IllegalArgumentException(
            "Missing JSON string: $name"
        )
}

internal fun String.requireJsonLong(
    name: String
): Long {
    return jsonNumber(name)
        ?: throw IllegalArgumentException(
            "Missing JSON number: $name"
        )
}

internal fun String.jsonString(
    name: String
): String? {
    val pattern =
        Regex(
            "\"${Regex.escape(name)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        )

    return pattern.find(this)
        ?.groupValues
        ?.get(1)
        ?.fromJsonEscaped()
}

internal fun String.jsonNumber(
    name: String
): Long? {
    val pattern =
        Regex(
            "\"${Regex.escape(name)}\"\\s*:\\s*(-?\\d+)"
        )

    return pattern.find(this)
        ?.groupValues
        ?.get(1)
        ?.toLongOrNull()
}

internal fun StringBuilder.appendJsonString(
    name: String,
    value: String
) {
    append("\"")
    append(name)
    append("\":")
    append("\"")
    append(value.toJsonEscaped())
    append("\"")
}

internal fun StringBuilder.appendJsonNumber(
    name: String,
    value: Number
) {
    append("\"")
    append(name)
    append("\":")
    append(value)
}

internal fun String.toJsonEscaped(): String {
    return buildString {
        this@toJsonEscaped.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

internal fun String.fromJsonEscaped(): String {
    val output =
        StringBuilder()

    var index = 0

    while (index < length) {
        val char =
            this[index]

        if (char != '\\' || index + 1 >= length) {
            output.append(char)
            index += 1
            continue
        }

        when (val escaped = this[index + 1]) {
            '\\' -> output.append('\\')
            '"' -> output.append('"')
            'n' -> output.append('\n')
            'r' -> output.append('\r')
            't' -> output.append('\t')
            else -> output.append(escaped)
        }

        index += 2
    }

    return output.toString()
}
