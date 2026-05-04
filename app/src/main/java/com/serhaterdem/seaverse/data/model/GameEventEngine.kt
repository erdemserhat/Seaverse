package com.serhaterdem.seaverse.data.model

import java.util.Locale
import kotlin.math.abs

object GameEventEngine {
    private const val EventIntervalSeconds = 60

    fun zoneForDepth(depthMeters: Int): String = when (depthMeters) {
        in 0..200 -> "surface"
        in 201..1000 -> "mid"
        in 1001..3000 -> "deep"
        else -> "abyss"
    }

    fun shouldTriggerEvent(
        state: GameState,
        elapsedSec: Int,
        hasActiveEvent: Boolean,
        isLoading: Boolean
    ): Boolean {
        if (hasActiveEvent || isLoading || elapsedSec < EventIntervalSeconds) return false
        if (elapsedSec - state.lastEventAtSeconds < EventIntervalSeconds) return false

        return elapsedSec % EventIntervalSeconds == 0
    }

    fun tickSurvival(state: GameState): GameState =
        state.copy(
            survivedSeconds = state.survivedSeconds + 1,
            score = state.score + 1
        )

    fun applyOption(state: GameState, option: Option): EventResult {
        val effects = option.effects.sanitized()
        val rawText = "${option.id} ${option.text}".lowercase(Locale.ROOT)
        val notes = mutableListOf<String>()

        var health = state.health + effects.health
        var comfort = state.comfort + effects.comfort
        var hunger = state.hunger + effects.hunger
        var energy = state.energy + effects.energy
        var score = state.score + effects.score

        if (rawText.contains("freshwater") || rawText.contains("tatlı su")) {
            health -= 20
            score -= 10
            notes += "Tatlı su seçimi sağlığı düşürdü; bu balık tuzlu su habitatına uyumlu."
        }

        if (state.zone == "abyss" && rawText.contains("surface")) {
            energy -= 8
            notes += "Çok hızlı yükselmek enerji kaybettirdi; derinlik değişimleri sakin yapılmalı."
        }

        hunger = hunger.coerceIn(0, 100)
        energy = energy.coerceIn(0, 100)
        comfort = comfort.coerceIn(0, 100)

        if (hunger > 80) {
            health -= 10
            notes += "Açlık kritik seviyede olunca sağlık azalmaya başladı."
        }

        if (energy < 15) {
            comfort -= 6
            notes += "Enerji çok düşünce rahatlık da azaldı."
        }

        health = health.coerceIn(0, 100)
        comfort = comfort.coerceIn(0, 100)

        val nextState = state.copy(
            health = health,
            comfort = comfort,
            hunger = hunger,
            energy = energy,
            score = score,
            zone = effects.nextZone ?: state.zone
        )

        val feedback = notes.firstOrNull() ?: when {
            effects.score > 0 && effects.hunger < 0 ->
                "İyi seçim: hem beslendin hem de habitatına uygun davrandın."

            effects.score > 0 ->
                "Mantıklı karar: puanın arttı ve deniz davranışını doğru okudun."

            effects.health < 0 || effects.score < 0 ->
                "Riskli seçim: okyanusta her kararın sağlık ve puan karşılığı var."

            else ->
                "Dengeli seçim: kısa vadede güvenli kaldın, ama kaynaklarını takip etmelisin."
        }

        return EventResult(
            state = nextState,
            feedback = feedback
        )
    }

    fun fallbackEvent(
        state: GameState,
        fishName: String,
        habitat: String,
        dietType: String
    ): Event {
        val zone = state.zone
        val pressure = when {
            state.hunger > 70 -> "Açlık bastırıyor; $fishName için $dietType beslenmeye uygun karar vermelisin."
            state.energy < 30 -> "Enerjin azaldı; $habitat içinde güvenli bir ritim bulmalısın."
            zone == "surface" -> "Işıklı bölgede hareket var; küçük bir karar seni öne geçirebilir."
            zone == "mid" -> "Işık azalıyor; av ve saklanma dengesi daha önemli hale geldi."
            zone == "deep" -> "Derin suda enerji tasarrufu hayatta kalmanın anahtarı."
            else -> "Karanlık dipte besin az; yavaş ve doğru karar vermelisin."
        }

        val options = when {
            state.hunger > 70 -> listOf(
                Option(
                    id = "hunt_suitable_food",
                    text = "Habitatındaki uygun küçük avı ara",
                    effects = Effects(hunger = -20, energy = -8, score = 12)
                ),
                Option(
                    id = "wait_in_cover",
                    text = "Saklanıp enerjini koru",
                    effects = Effects(hunger = 8, energy = 6, score = 2)
                ),
                Option(
                    id = "go_freshwater",
                    text = "Tatlı suya doğru yüz",
                    effects = Effects(health = -12, score = -8)
                )
            )

            state.energy < 30 -> listOf(
                Option(
                    id = "rest_near_shelter",
                    text = "Güvenli bir çıkıntıda dinlen",
                    effects = Effects(energy = 10, hunger = 4, score = 8)
                ),
                Option(
                    id = "fast_chase",
                    text = "Hızlı bir kovalamacaya gir",
                    effects = Effects(energy = -16, hunger = -10, score = 5)
                ),
                Option(
                    id = "ignore_fatigue",
                    text = "Yorgunluğu önemsemeden devam et",
                    effects = Effects(health = -10, energy = -8, score = -6)
                )
            )

            abs(state.depth) < 220 -> listOf(
                Option(
                    id = "inspect_reef",
                    text = "Resifin kenarında yiyecek izi ara",
                    effects = Effects(hunger = -10, energy = -5, score = 10)
                ),
                Option(
                    id = "follow_shadow",
                    text = "Büyük gölgeyi takip et",
                    effects = Effects(health = -8, energy = -8, score = -4)
                )
            )

            else -> listOf(
                Option(
                    id = "slow_deep_swim",
                    text = "Yavaş yüzüp enerjini koru",
                    effects = Effects(energy = 6, hunger = 4, score = 8)
                ),
                Option(
                    id = "chase_glow",
                    text = "Parlayan canlıların peşine düş",
                    effects = Effects(hunger = -12, energy = -12, score = 10)
                ),
                Option(
                    id = "rush_surface",
                    text = "Hemen yüzeye fırla",
                    effects = Effects(comfort = -16, energy = -12, score = -8)
                )
            )
        }

        return Event(
            id = "local_${state.fishId}_${state.zone}_${state.survivedSeconds}",
            text = pressure,
            options = options,
            source = EventSource.Local
        )
    }

    private fun Effects.sanitized(): Effects =
        copy(
            health = health.coerceIn(-20, 10),
            hunger = hunger.coerceIn(-20, 20),
            energy = energy.coerceIn(-20, 10),
            score = score.coerceIn(-10, 15),
            comfort = comfort.coerceIn(-20, 10),
            nextZone = nextZone?.takeIf { it in setOf("surface", "mid", "deep", "abyss") }
        )
}
