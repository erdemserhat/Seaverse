package com.serhaterdem.seaverse.data.repository

import com.serhaterdem.seaverse.data.model.ConservationStatus
import com.serhaterdem.seaverse.data.model.Diet
import com.serhaterdem.seaverse.data.model.Fish
import com.serhaterdem.seaverse.data.model.Habitat

class FishRepository {

    fun getFishById(id: String): Fish? = fishDatabase.find { it.id == id }

    fun getAllFish(): List<Fish> = fishDatabase.toList()

    fun getFishByHabitat(habitat: Habitat): List<Fish> =
        fishDatabase.filter { it.habitat == habitat }

    companion object {
        private val fishDatabase = listOf(
            Fish(
                id = "clownfish",
                name = "Clownfish",
                scientificName = "Amphiprioninae",
                habitat = Habitat.CORAL_REEF,
                diet = Diet.OMNIVORE,
                description = "A small, colorful fish known for its symbiotic relationship with sea anemones.",
                imageResId = 0,
                threats = listOf("Coral bleaching", "Ocean acidification", "Collection for aquarium trade"),
                funFacts = listOf(
                    "All clownfish are born male and can change to female",
                    "They are immune to anemone stings",
                    "A group of clownfish is led by a dominant female"
                ),
                speedKmh = 4f,
                maxDepthMeters = 15,
                conservationStatus = ConservationStatus.LEAST_CONCERN
            ),
            Fish(
                id = "bluefin_tuna",
                name = "Bluefin Tuna",
                scientificName = "Thunnus thynnus",
                habitat = Habitat.OPEN_OCEAN,
                diet = Diet.CARNIVORE,
                description = "One of the fastest and largest bony fish in the ocean.",
                imageResId = 0,
                threats = listOf("Overfishing", "Bycatch", "Habitat degradation"),
                funFacts = listOf(
                    "Can swim up to 70 km/h",
                    "They can regulate their body temperature",
                    "A single bluefin tuna can weigh over 600 kg"
                ),
                speedKmh = 70f,
                maxDepthMeters = 1000,
                conservationStatus = ConservationStatus.ENDANGERED
            ),
            Fish(
                id = "seahorse",
                name = "Seahorse",
                scientificName = "Hippocampus",
                habitat = Habitat.COASTAL,
                diet = Diet.CARNIVORE,
                description = "A unique fish known for its horse-like head and upright swimming posture.",
                imageResId = 0,
                threats = listOf("Habitat loss", "Traditional medicine trade", "Bycatch"),
                funFacts = listOf(
                    "Males carry and give birth to babies",
                    "They have no stomach and must eat constantly",
                    "Their eyes can move independently"
                ),
                speedKmh = 0.001f,
                maxDepthMeters = 45,
                conservationStatus = ConservationStatus.VULNERABLE
            ),
            Fish(
                id = "anglerfish",
                name = "Anglerfish",
                scientificName = "Lophiiformes",
                habitat = Habitat.DEEP_SEA,
                diet = Diet.CARNIVORE,
                description = "A deep-sea predator that uses a bioluminescent lure to attract prey.",
                imageResId = 0,
                threats = listOf("Deep-sea trawling", "Climate change"),
                funFacts = listOf(
                    "Males fuse to females in a parasitic relationship",
                    "Their bioluminescent lure is produced by bacteria",
                    "They can swallow prey twice their size"
                ),
                speedKmh = 0.5f,
                maxDepthMeters = 4000,
                conservationStatus = ConservationStatus.LEAST_CONCERN
            ),
            Fish(
                id = "great_white_shark",
                name = "Great White Shark",
                scientificName = "Carcharodon carcharias",
                habitat = Habitat.OPEN_OCEAN,
                diet = Diet.CARNIVORE,
                description = "The largest predatory fish in the ocean, known for powerful jaws and keen senses.",
                imageResId = 0,
                threats = listOf("Bycatch", "Shark finning", "Fear-based culling"),
                funFacts = listOf(
                    "They can detect a single drop of blood in 100 liters of water",
                    "Great whites can live up to 70 years",
                    "They breach the surface when hunting seals"
                ),
                speedKmh = 56f,
                maxDepthMeters = 1200,
                conservationStatus = ConservationStatus.VULNERABLE
            )
        )
    }
}
