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

        val engineFeedback = notes.firstOrNull()
        val optionLesson = option.lesson.trim()
        val feedback = when {
            engineFeedback != null && optionLesson.isNotBlank() -> "$engineFeedback $optionLesson"
            engineFeedback != null -> engineFeedback
            optionLesson.isNotBlank() -> optionLesson
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
            feedback = feedback,
            assessment = assessChoice(state, nextState)
        )
    }

    private fun assessChoice(
        before: GameState,
        after: GameState
    ): ChoiceAssessment {
        val scoreDelta = after.score - before.score
        val healthDelta = after.health - before.health
        val comfortDelta = after.comfort - before.comfort
        val energyDelta = after.energy - before.energy
        val hungerDelta = after.hunger - before.hunger

        return when {
            scoreDelta >= 10 &&
                healthDelta >= 0 &&
                comfortDelta >= -2 &&
                hungerDelta <= 0 -> ChoiceAssessment.Correct

            healthDelta <= -12 ||
                comfortDelta <= -14 ||
                scoreDelta <= -6 -> ChoiceAssessment.Wrong

            healthDelta < 0 ||
                comfortDelta < 0 ||
                energyDelta <= -12 -> ChoiceAssessment.Risky

            scoreDelta >= 5 -> ChoiceAssessment.Partial
            else -> ChoiceAssessment.Partial
        }
    }

    fun fallbackEvent(
        state: GameState,
        fishName: String,
        habitat: String,
        dietType: String
    ): Event {
        val zone = state.zone
        val pressure = when {
            state.hunger > 70 -> "Bilgi görevi: $fishName açken $dietType beslenme için hangi davranış habitatına daha uygundur?"
            state.energy < 30 -> "Bilgi görevi: $habitat içinde enerji azaldığında hangi okyanus davranışı daha güvenlidir?"
            zone == "surface" -> "Bilgi görevi: Işıklı bölgede avcılar ve saklanma alanları varken ne yapmak daha doğrudur?"
            zone == "mid" -> "Bilgi görevi: Işık azalırken av ve sığınak dengesi için hangi seçim daha uygundur?"
            zone == "deep" -> "Bilgi görevi: Derin suda basınç ve az besin varken hangi strateji daha doğrudur?"
            else -> "Bilgi görevi: Karanlık dipte enerji çok değerliyken hangi karar daha güvenlidir?"
        }

        val options = when {
            state.hunger > 70 -> listOf(
                Option(
                    id = "hunt_suitable_food",
                    text = "Habitatındaki uygun küçük avı ara",
                    effects = Effects(hunger = -20, energy = -8, score = 12),
                    lesson = "$fishName için doğru besini kendi habitatında aramak açlığı azaltır ve gereksiz riskleri düşürür."
                ),
                Option(
                    id = "wait_in_cover",
                    text = "Saklanıp enerjini koru",
                    effects = Effects(hunger = 8, energy = 6, score = 2),
                    lesson = "Saklanmak enerji kazandırır ama açlık artacağı için uzun süre tek başına yeterli değildir."
                ),
                Option(
                    id = "go_freshwater",
                    text = "Tatlı suya doğru yüz",
                    effects = Effects(health = -12, score = -8),
                    lesson = "Deniz balıkları tatlı suya uyumlu değildir; tuzluluk değişimi sağlık için tehlikelidir."
                )
            )

            state.energy < 30 -> listOf(
                Option(
                    id = "rest_near_shelter",
                    text = "Güvenli bir çıkıntıda dinlen",
                    effects = Effects(energy = 10, hunger = 4, score = 8),
                    lesson = "Sığınak yakınında dinlenmek avcılardan uzak kalırken enerjiyi toparlamaya yardım eder."
                ),
                Option(
                    id = "fast_chase",
                    text = "Hızlı bir kovalamacaya gir",
                    effects = Effects(energy = -16, hunger = -10, score = 5),
                    lesson = "Kovalamaca besin sağlayabilir ama düşük enerjide fazla hareket daha büyük risk oluşturur."
                ),
                Option(
                    id = "ignore_fatigue",
                    text = "Yorgunluğu önemsemeden devam et",
                    effects = Effects(health = -10, energy = -8, score = -6),
                    lesson = "Yorgunluğu yok saymak balığın kaçma ve yön bulma becerisini zayıflatır."
                )
            )

            abs(state.depth) < 220 -> listOf(
                Option(
                    id = "inspect_reef",
                    text = "Resifin kenarında yiyecek izi ara",
                    effects = Effects(hunger = -10, energy = -5, score = 10),
                    lesson = "Resif kenarları küçük canlılar ve saklanma boşlukları sunduğu için güvenli beslenme alanıdır."
                ),
                Option(
                    id = "follow_shadow",
                    text = "Büyük gölgeyi takip et",
                    effects = Effects(health = -8, energy = -8, score = -4),
                    lesson = "Büyük gölgeler çoğu zaman avcı veya tehlikeli büyük canlı olabilir; yaklaşmak risklidir."
                )
            )

            else -> listOf(
                Option(
                    id = "slow_deep_swim",
                    text = "Yavaş yüzüp enerjini koru",
                    effects = Effects(energy = 6, hunger = 4, score = 8),
                    lesson = "Derin suda besin az olduğu için yavaş hareket etmek enerji tasarrufu sağlar."
                ),
                Option(
                    id = "chase_glow",
                    text = "Parlayan canlıların peşine düş",
                    effects = Effects(hunger = -12, energy = -12, score = 10),
                    lesson = "Biyolüminesans besin işareti olabilir ama peşinden gitmek enerji maliyeti ve avcı riski taşır."
                ),
                Option(
                    id = "rush_surface",
                    text = "Hemen yüzeye fırla",
                    effects = Effects(comfort = -16, energy = -12, score = -8),
                    lesson = "Derinden hızlı yükselmek basınç ve enerji dengesini bozabileceği için yanlış bir tepkidir."
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
