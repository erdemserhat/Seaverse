package com.serhaterdem.seaverse.data.model

data class Scenario(
    val id: String,
    val title: String,
    val description: String,
    val type: ScenarioType,
    val choices: List<Choice>,
    val requiredHabitat: Habitat? = null,
    val timeLimitSeconds: Int = 30
)

data class Choice(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val scoreModifier: Int,
    val resultDescription: String,
    val learningNote: String,
    val nextScenarioId: String? = null
)

enum class ScenarioType {
    PREDATOR_ENCOUNTER,
    HABITAT_CHANGE,
    FOOD_SCARCITY,
    POLLUTION,
    HUMAN_THREAT,
    MIGRATION,
    REPRODUCTION
}
