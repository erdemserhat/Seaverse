package com.serhaterdem.seaverse.data.qr

import com.serhaterdem.seaverse.ui.game.PlayableFish

object QrFishResolver {
    private const val PREFIX = "seaverse://fish/"

    internal fun resolve(raw: String, roster: List<PlayableFish>): PlayableFish? {
        val id = when {
            raw.startsWith(PREFIX) -> raw.removePrefix(PREFIX).trim()
            else -> raw.trim()
        }
        return roster.find { it.id == id }
    }
}
