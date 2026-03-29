package com.serhaterdem.seaverse.data.model

data class GameState(
    val playerFish: Fish? = null,
    val currentScenario: Scenario? = null,
    val score: Int = 0,
    val health: Int = 100,
    val scenariosCompleted: Int = 0,
    val totalDecisions: Int = 0,
    val correctDecisions: Int = 0,
    val isGameOver: Boolean = false,
    val timeRemainingSeconds: Int = 30,
    val learnedFacts: List<String> = emptyList()
) {
    val accuracyPercentage: Float
        get() = if (totalDecisions > 0) {
            (correctDecisions.toFloat() / totalDecisions) * 100f
        } else 0f

    val survivalBonus: Int
        get() = if (!isGameOver) health * 2 else 0
}
