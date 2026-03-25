package com.serhaterdem.seaverse.data.model

data class Fish(
    val id: String,
    val name: String,
    val scientificName: String,
    val habitat: Habitat,
    val diet: Diet,
    val description: String,
    val imageResId: Int,
    val threats: List<String>,
    val funFacts: List<String>,
    val speedKmh: Float,
    val maxDepthMeters: Int,
    val conservationStatus: ConservationStatus
)

enum class Habitat {
    CORAL_REEF, DEEP_SEA, OPEN_OCEAN, COASTAL, FRESHWATER, ESTUARY
}

enum class Diet {
    HERBIVORE, CARNIVORE, OMNIVORE, FILTER_FEEDER, PARASITIC
}

enum class ConservationStatus {
    LEAST_CONCERN, NEAR_THREATENED, VULNERABLE, ENDANGERED, CRITICALLY_ENDANGERED
}
