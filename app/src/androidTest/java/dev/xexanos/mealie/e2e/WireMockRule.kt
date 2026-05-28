package dev.xexanos.mealie.e2e

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.HttpURLConnection
import java.net.URL

class WireMockRule(
    private val host: String = "10.0.2.2",
    private val port: Int = 8080,
) : TestRule {

    val baseUrl: String get() = "http://$host:$port"
    private val adminUrl: String get() = "$baseUrl/__admin"

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                reset()
                try {
                    base.evaluate()
                } finally {
                    reset()
                }
            }
        }
    }

    fun reset() {
        post("$adminUrl/reset", "")
    }

    fun stubFor(mappingJson: String) {
        post("$adminUrl/mappings", mappingJson)
    }

    fun stubAppAboutSuccess() {
        stubFor("""
            {
              "request": {
                "method": "GET",
                "urlPath": "/api/app/about"
              },
              "response": {
                "status": 200,
                "jsonBody": {"version": "v3.0.0"},
                "headers": {"Content-Type": "application/json"}
              }
            }
        """.trimIndent())
    }

    fun stubAppAboutNotMealie() {
        stubFor("""
            {
              "request": {
                "method": "GET",
                "urlPath": "/api/app/about"
              },
              "response": {
                "status": 200,
                "jsonBody": {"version": "v2.0.0"},
                "headers": {"Content-Type": "application/json"}
              }
            }
        """.trimIndent())
    }

    fun stubAppAboutWithDelay(delayMs: Int) {
        stubFor("""
            {
              "request": {
                "method": "GET",
                "urlPath": "/api/app/about"
              },
              "response": {
                "status": 200,
                "jsonBody": {"version": "3.0.0"},
                "headers": {"Content-Type": "application/json"},
                "fixedDelayMilliseconds": $delayMs
              }
            }
        """.trimIndent())
    }

    fun stubAuthSuccess() {
        stubFor("""
            {
              "request": {
                "method": "POST",
                "urlPath": "/api/auth/token"
              },
              "response": {
                "status": 200,
                "jsonBody": {"access_token": "test-token-abc123", "token_type": "bearer"},
                "headers": {"Content-Type": "application/json"}
              }
            }
        """.trimIndent())
    }

    fun stubAuthUnauthorized() {
        stubFor("""
            {
              "request": {
                "method": "POST",
                "urlPath": "/api/auth/token"
              },
              "response": {
                "status": 401,
                "jsonBody": {"detail": "Unauthorized"},
                "headers": {"Content-Type": "application/json"}
              }
            }
        """.trimIndent())
    }

    private fun post(url: String, body: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doOutput = true
        try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            check(code in 200..299) {
                "WireMock admin API returned $code for $url"
            }
        } finally {
            connection.disconnect()
        }
    }
}
