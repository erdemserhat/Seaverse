package com.serhaterdem.seaverse

import com.serhaterdem.seaverse.data.model.Effects
import com.serhaterdem.seaverse.data.model.GameEventEngine
import com.serhaterdem.seaverse.data.model.GameState
import com.serhaterdem.seaverse.data.model.Option
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEventEngineTest {
    @Test
    fun applyOption_clampsStatsAndScoreEffects() {
        val state = GameState(
            fishId = "sunburst",
            health = 96,
            comfort = 95,
            hunger = 8,
            energy = 96,
            score = 10
        )
        val option = Option(
            id = "big_reward",
            text = "Güvenli avı yakala",
            effects = Effects(
                health = 99,
                comfort = 99,
                hunger = -99,
                energy = 99,
                score = 99
            )
        )

        val result = GameEventEngine.applyOption(state, option).state

        assertEquals(100, result.health)
        assertEquals(100, result.comfort)
        assertEquals(0, result.hunger)
        assertEquals(100, result.energy)
        assertEquals(25, result.score)
    }

    @Test
    fun applyOption_addsFreshwaterPenaltyForMarineFish() {
        val state = GameState(
            fishId = "reef_guardian",
            health = 80,
            hunger = 50,
            energy = 80,
            score = 20
        )
        val option = Option(
            id = "go_freshwater",
            text = "Tatlı suya doğru yüz",
            effects = Effects(health = -5, score = -4)
        )

        val result = GameEventEngine.applyOption(state, option)

        assertEquals(55, result.state.health)
        assertEquals(6, result.state.score)
        assertTrue(result.feedback.contains("Tatlı su"))
    }

    @Test
    fun shouldTriggerEvent_firesOncePerMinuteOnly() {
        val coolingDown = GameState(
            fishId = "lantern_dart",
            hunger = 90,
            energy = 20,
            lastEventAtSeconds = 30
        )
        val ready = coolingDown.copy(lastEventAtSeconds = 0)

        assertFalse(
            GameEventEngine.shouldTriggerEvent(
                state = coolingDown,
                elapsedSec = 60,
                hasActiveEvent = false,
                isLoading = false
            )
        )
        assertFalse(
            GameEventEngine.shouldTriggerEvent(
                state = ready,
                elapsedSec = 59,
                hasActiveEvent = false,
                isLoading = false
            )
        )
        assertTrue(
            GameEventEngine.shouldTriggerEvent(
                state = ready,
                elapsedSec = 60,
                hasActiveEvent = false,
                isLoading = false
            )
        )
    }
}
