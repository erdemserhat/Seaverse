package com.serhaterdem.seaverse.data.remote

import com.serhaterdem.seaverse.BuildConfig
import com.serhaterdem.seaverse.data.model.Effects
import com.serhaterdem.seaverse.data.model.Event
import com.serhaterdem.seaverse.data.model.EventSource
import com.serhaterdem.seaverse.data.model.FishEventContext
import com.serhaterdem.seaverse.data.model.Option
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object OpenAiFishEventClient {
    private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    private const val MODEL = "gpt-5.4-mini"

    suspend fun generateEvent(context: FishEventContext): Result<Event> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = BuildConfig.OPENAI_API_KEY
            check(apiKey.isNotBlank()) {
                "OPENAI_API_KEY local.properties içinde bulunamadı."
            }

            val requestBody = JSONObject()
                .put("model", MODEL)
                .put("instructions", eventInstructions())
                .put("max_output_tokens", 360)
                .put("store", false)
                .put("input", buildEventInput(context))

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

            parseEvent(
                rawText = parseAssistantText(JSONObject(responseText)),
                fallbackId = "llm_${context.fishId}_${context.gameState.zone}_${context.gameState.survivedSeconds}"
            )
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    private fun eventInstructions(): String = """
        You create dynamic educational events for a 2D underwater survival game.
        Create ONE short event/question for a child age 7-12.
        Return STRICT JSON only, no markdown and no explanation.
        JSON shape:
        {
          "text": "one short Turkish sentence",
          "options": [
            {
              "id": "short_snake_case_id",
              "text": "Turkish choice text",
              "effects": {
                "health": 0,
                "hunger": 0,
                "energy": 0,
                "comfort": 0,
                "score": 0,
                "nextZone": null
              }
            }
          ]
        }
        Use 2 or 3 options.
        Numeric ranges:
        health -20..10, hunger -20..20, energy -20..10, comfort -20..10, score -10..15.
        Lower hunger is good. Higher energy, comfort, health, and score are good.
        Make the event highly specific to the given fish name, habitat, diet, food, depth range, and ecological role.
        Include concrete ocean situations such as currents, predators, prey behavior, reef damage, pollution, low light, pressure, temperature, shelters, or migration.
        Do not ask a generic ocean question when the fish profile supports a more specific survival decision.
        Avoid repeating any previous event/question given in the input.
        Include at least one tempting but biologically risky option sometimes.
    """.trimIndent()

    private fun buildEventInput(context: FishEventContext): String = """
        Fish: ${context.fishName}
        Fish id: ${context.fishId}
        Habitat: ${context.habitat}
        Personality: ${context.personality}
        Depth range: ${context.depthRange}
        Diet type: ${context.dietType}
        Food: ${context.food}
        Ecological role: ${context.ecologicalRole}

        Current state:
        depth: ${context.gameState.depth}m
        zone: ${context.gameState.zone}
        health: ${context.gameState.health}
        comfort: ${context.gameState.comfort}
        hunger: ${context.gameState.hunger}
        energy: ${context.gameState.energy}
        score: ${context.gameState.score}

        Previous events/questions to avoid:
        ${context.previousEventTexts.takeLast(8).ifEmpty { listOf("None") }.joinToString(separator = "\n") { "- $it" }}

        Write the event and options in Turkish.
    """.trimIndent()

    private fun parseEvent(rawText: String, fallbackId: String): Event {
        val json = JSONObject(extractJsonObject(rawText))
        val optionsJson = json.getJSONArray("options")
        require(optionsJson.length() in 2..3) {
            "LLM event 2 veya 3 seçenek içermeli."
        }

        val options = mutableListOf<Option>()
        for (index in 0 until optionsJson.length()) {
            val optionJson = optionsJson.getJSONObject(index)
            val effectsJson = optionJson.optJSONObject("effects") ?: JSONObject()
            options += Option(
                id = optionJson.optString("id").ifBlank { "option_$index" },
                text = optionJson.getString("text"),
                effects = Effects(
                    health = effectsJson.optInt("health", 0),
                    hunger = effectsJson.optInt("hunger", 0),
                    energy = effectsJson.optInt("energy", 0),
                    comfort = effectsJson.optInt("comfort", 0),
                    score = effectsJson.optInt("score", 0),
                    nextZone = effectsJson.optString("nextZone")
                        .takeUnless { it.isBlank() || it == "null" }
                )
            )
        }

        return Event(
            id = json.optString("id").ifBlank { fallbackId },
            text = json.getString("text"),
            options = options,
            source = EventSource.Llm
        )
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) {
            "LLM yanıtında JSON nesnesi bulunamadı."
        }
        return text.substring(start, end + 1)
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
            "OpenAI event isteği başarısız oldu. Kod: $statusCode"
        } else {
            "OpenAI event isteği başarısız oldu. Kod: $statusCode - $message"
        }
    }
}
