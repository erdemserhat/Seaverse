package com.serhaterdem.seaverse.data.model

data class Event(
    val id: String,
    val text: String,
    val options: List<Option>,
    val source: EventSource = EventSource.Local
)

data class Option(
    val id: String,
    val text: String,
    val effects: Effects,
    val lesson: String = ""
)

data class Effects(
    val health: Int = 0,
    val hunger: Int = 0,
    val energy: Int = 0,
    val score: Int = 0,
    val comfort: Int = 0,
    val nextZone: String? = null
)

enum class EventSource {
    Llm,
    Local,
    Cache
}

data class EventResult(
    val state: GameState,
    val feedback: String,
    val assessment: ChoiceAssessment
)

enum class ChoiceAssessment(val label: String) {
    Correct("Doğru"),
    Partial("Kısmen doğru"),
    Risky("Riskli"),
    Wrong("Yanlış")
}

data class FishEventContext(
    val fishId: String,
    val fishName: String,
    val habitat: String,
    val personality: String,
    val depthRange: String,
    val dietType: String,
    val food: String,
    val ecologicalRole: String,
    val gameState: GameState,
    val previousEventTexts: List<String> = emptyList()
)
