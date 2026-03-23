package com.serhaterdem.seaverse.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Game : Screen("game/{fishId}") {
        fun createRoute(fishId: String) = "game/$fishId"
    }
    data object Result : Screen("result/{score}") {
        fun createRoute(score: Int) = "result/$score"
    }
    data object Encyclopedia : Screen("encyclopedia")
    data object FishDetail : Screen("fish/{fishId}") {
        fun createRoute(fishId: String) = "fish/$fishId"
    }
}
