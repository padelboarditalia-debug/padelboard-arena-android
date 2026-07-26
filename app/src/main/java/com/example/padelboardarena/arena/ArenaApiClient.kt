package com.example.padelboardarena.arena

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ArenaApiClient(
    private val config: ArenaConfig,
    private val tokenProvider: ArenaTokenProvider,
    private val transport: ArenaHttpTransport =
        UrlConnectionArenaHttpTransport()
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    fun sendState(
        snapshot: ArenaScoreSnapshot,
        callback: (ArenaApiResult) -> Unit
    ) {
        executor.execute {
            callback(
                sendStateBlocking(
                    snapshot = snapshot
                )
            )
        }
    }

    fun sendStateBlocking(
        snapshot: ArenaScoreSnapshot
    ): ArenaApiResult {
        config.requireComplete()

        val accessToken =
            tokenProvider.currentAccessToken()
                ?: return ArenaApiResult.fromHttp(
                    statusCode = 401,
                    body = "Missing access token"
                )

        val firstResult =
            postState(
                snapshot = snapshot,
                accessToken = accessToken
            )

        if (firstResult.statusCode != 401) {
            return firstResult
        }

        val refreshedToken =
            tokenProvider.refreshAccessToken()
                ?: return firstResult

        return postState(
            snapshot = snapshot,
            accessToken = refreshedToken
        )
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    fun stateUrl(): String {
        return "${config.apiBaseUrl.trimEnd('/')}" +
                "/api/arena/courts/" +
                config.courtId.trim('/') +
                "/state"
    }

    private fun postState(
        snapshot: ArenaScoreSnapshot,
        accessToken: String
    ): ArenaApiResult {
        val response =
            transport.execute(
                ArenaHttpRequest(
                    method = "POST",
                    url = stateUrl(),
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to "Bearer $accessToken"
                    ),
                    body = snapshot.toJson()
                )
            )

        return ArenaApiResult.fromHttp(
            statusCode = response.statusCode,
            body = response.body
        )
    }
}
