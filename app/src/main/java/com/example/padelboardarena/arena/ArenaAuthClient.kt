package com.example.padelboardarena.arena

class ArenaAuthClient(
    private val config: ArenaConfig,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport(),
    private val clockMillis: () -> Long =
        { System.currentTimeMillis() }
) : ArenaTokenProvider {
    @Volatile
    private var session: ArenaAuthSession? = null

    fun login(
        password: String
    ): ArenaAuthSession {
        config.requireComplete()

        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "POST",
                    url = tokenUrl(
                        grantType = "password"
                    ),
                    headers = authHeaders(),
                    body = passwordBody(
                        email = config.email,
                        password = password
                    )
                )
            )

        require(response.statusCode in 200..299) {
            "Supabase login failed: HTTP ${response.statusCode}"
        }

        return ArenaAuthSession.fromJson(
            body = response.body,
            receivedAtMillis = clockMillis()
        ).also { newSession ->
            session = newSession
        }
    }

    override fun currentAccessToken(): String? {
        return session?.accessToken
    }

    override fun refreshAccessToken(): String? {
        val refreshToken =
            session?.refreshToken
                ?: return null

        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "POST",
                    url = tokenUrl(
                        grantType = "refresh_token"
                    ),
                    headers = authHeaders(),
                    body = refreshBody(
                        refreshToken = refreshToken
                    )
                )
            )

        if (response.statusCode !in 200..299) {
            return null
        }

        val refreshedSession =
            ArenaAuthSession.fromJson(
                body = response.body,
                receivedAtMillis = clockMillis()
            )

        session = refreshedSession

        return refreshedSession.accessToken
    }

    private fun authHeaders(): Map<String, String> {
        return mapOf(
            "apikey" to config.supabasePublishableKey,
            "Content-Type" to "application/json"
        )
    }

    private fun tokenUrl(
        grantType: String
    ): String {
        return "${config.supabaseUrl.trimEnd('/')}" +
                "/auth/v1/token?grant_type=$grantType"
    }

    private fun passwordBody(
        email: String,
        password: String
    ): String {
        return buildString {
            append("{")
            appendJsonString("email", email)
            append(",")
            appendJsonString("password", password)
            append("}")
        }
    }

    private fun refreshBody(
        refreshToken: String
    ): String {
        return buildString {
            append("{")
            appendJsonString("refresh_token", refreshToken)
            append("}")
        }
    }
}

interface ArenaTokenProvider {
    fun currentAccessToken(): String?

    fun refreshAccessToken(): String?
}
