package com.example.padelboardarena.arena

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ArenaHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String
)

data class ArenaHttpResponse(
    val statusCode: Int,
    val body: String
)

interface ArenaHttpTransport {
    fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse
}

class UrlConnectionArenaHttpTransport : ArenaHttpTransport {
    override fun execute(
        request: ArenaHttpRequest
    ): ArenaHttpResponse {
        var connection: HttpURLConnection? = null

        try {
            connection =
                URL(request.url).openConnection()
                    as HttpURLConnection

            connection.requestMethod = request.method
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = request.body.isNotEmpty()

            request.headers.forEach { header ->
                connection.setRequestProperty(
                    header.key,
                    header.value
                )
            }

            if (request.body.isNotEmpty()) {
                OutputStreamWriter(
                    connection.outputStream,
                    Charsets.UTF_8
                ).use { writer ->
                    writer.write(request.body)
                }
            }

            val statusCode =
                connection.responseCode

            val responseBody =
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }?.bufferedReader()
                    ?.use { reader ->
                        reader.readText()
                    }
                    .orEmpty()

            return ArenaHttpResponse(
                statusCode = statusCode,
                body = responseBody
            )
        } finally {
            connection?.disconnect()
        }
    }
}
