package com.serhaterdem.seaverse.data.remote

import com.serhaterdem.seaverse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class FishChatRole {
    User,
    Fish
}

data class FishChatMessage(
    val role: FishChatRole,
    val text: String
)

object OpenAiFishChatClient {
    private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    private const val MODEL = "gpt-5.4-mini"

    suspend fun sendMessage(
        personaPrompt: String,
        history: List<FishChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = BuildConfig.OPENAI_API_KEY
            check(apiKey.isNotBlank()) {
                "OPENAI_API_KEY local.properties içinde bulunamadı."
            }

            val requestBody = JSONObject()
                .put("model", MODEL)
                .put("instructions", personaPrompt)
                .put("max_output_tokens", 220)
                .put("input", buildInput(history, userMessage))

            val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
            }

            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseText = readResponse(connection)
            if (connection.responseCode !in 200..299) {
                error(openAiErrorMessage(connection.responseCode, responseText))
            }

            parseAssistantText(JSONObject(responseText))
                .ifBlank { "Baloncuklarımı toparlayamadım. Bunu tekrar sorar mısın?" }
        }
    }

    private fun buildInput(
        history: List<FishChatMessage>,
        userMessage: String
    ): JSONArray {
        val input = JSONArray()
        history.takeLast(8).forEach { message ->
            input.put(
                JSONObject()
                    .put("role", if (message.role == FishChatRole.User) "user" else "assistant")
                    .put("content", message.text)
            )
        }
        input.put(
            JSONObject()
                .put("role", "user")
                .put("content", userMessage)
        )
        return input
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return stream.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
    }

    private fun parseAssistantText(response: JSONObject): String {
        response.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }

        val output = response.optJSONArray("output") ?: return ""
        val builder = StringBuilder()
        for (outputIndex in 0 until output.length()) {
            val item = output.optJSONObject(outputIndex) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val contentItem = content.optJSONObject(contentIndex) ?: continue
                if (contentItem.optString("type") == "output_text") {
                    builder.append(contentItem.optString("text")).append('\n')
                }
            }
        }
        return builder.toString().trim()
    }

    private fun openAiErrorMessage(statusCode: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()

        return if (message.isBlank()) {
            "OpenAI isteği başarısız oldu. Kod: $statusCode"
        } else {
            "OpenAI isteği başarısız oldu. Kod: $statusCode - $message"
        }
    }
}
