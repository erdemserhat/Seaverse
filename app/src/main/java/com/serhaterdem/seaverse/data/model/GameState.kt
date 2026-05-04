package com.serhaterdem.seaverse.data.model

data class GameState(
    val fishId: String = "",
    val health: Int = 100,
    val comfort: Int = 100,
    val hunger: Int = 50,
    val energy: Int = 100,
    val depth: Int = 0,
    val score: Int = 0,
    val zone: String = "surface",
    val survivedSeconds: Int = 0,
    val lastEventAtSeconds: Int = 0
)
