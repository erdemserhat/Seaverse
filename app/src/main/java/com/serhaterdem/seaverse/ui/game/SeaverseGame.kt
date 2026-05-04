package com.serhaterdem.seaverse.ui.game

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serhaterdem.seaverse.R
import com.serhaterdem.seaverse.data.model.ChoiceAssessment
import com.serhaterdem.seaverse.data.model.Event
import com.serhaterdem.seaverse.data.model.FishEventContext
import com.serhaterdem.seaverse.data.model.GameEventEngine
import com.serhaterdem.seaverse.data.model.GameState
import com.serhaterdem.seaverse.data.model.Option
import com.serhaterdem.seaverse.data.remote.FishChatMessage
import com.serhaterdem.seaverse.data.remote.FishChatRole
import com.serhaterdem.seaverse.data.remote.OpenAiFishChatClient
import com.serhaterdem.seaverse.data.remote.OpenAiFishEventClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MaxDiveDepthMeters = 4000f
private const val WorldDepthScreens = 7.5f
private const val WorldWidthScreens = 3.2f
private const val PredatorDepthStartMeters = 900f

private data class PlayableFish(
    val id: String,
    val name: String,
    val habitat: String,
    val personality: String,
    @param:DrawableRes val imageRes: Int,
    val accent: Color,
    val speedPxPerSecond: Float,
    val agility: Float,
    val stamina: Float
)

private data class FishInfo(
    val name: String,
    val habitat: String,
    val depthRange: String,
    val dietType: String,
    val food: String,
    val ecologicalRole: String,
    val note: String,
    val accent: Color
)

private enum class EventLogType(val label: String) {
    Info("Bilgi"),
    Warning("Uyarı"),
    Ideal("İdeal"),
    Extra("Ekstra")
}

private data class EventLogEntry(
    val type: EventLogType,
    val message: String
)

private data class LearningInfoCard(
    val title: String,
    val message: String,
    val type: EventLogType
)

private data class ChoiceRecord(
    val eventText: String,
    val optionText: String,
    val feedback: String,
    val assessment: ChoiceAssessment,
    val scoreDelta: Int,
    val healthDelta: Int,
    val comfortDelta: Int,
    val hungerDelta: Int,
    val energyDelta: Int,
    val depthMeters: Int
)

private data class ComfortRange(
    val label: String,
    val maxComfortDepthMeters: Float
)

private data class DepthZone(
    val name: String,
    val startMeters: Float,
    val description: String,
    val color: Color
)

private data class AmbientCreature(
    val name: String,
    val depthMeters: Float,
    @param:DrawableRes val imageRes: Int,
    val sizeDp: Float,
    val speed: Float,
    val phase: Float,
    val swimsRight: Boolean,
    val verticalDriftPx: Float
)

private enum class AmbientCreatureRole {
    Prey,
    Predator,
    Neutral
}

private data class DepthDecoration(
    val id: String,
    val depthMeters: Float,
    val xFraction: Float,
    @param:DrawableRes val imageRes: Int,
    val widthDp: Float,
    val alpha: Float,
    val parallax: Float,
    val anchoredToFloor: Boolean = true
)

private val DepthZones = listOf(
    DepthZone(
        name = "Sunlight Zone",
        startMeters = 0f,
        description = "Bright reef water",
        color = Color(0xFF6FE7FF)
    ),
    DepthZone(
        name = "Twilight Zone",
        startMeters = 200f,
        description = "Blue light fades",
        color = Color(0xFF2F80ED)
    ),
    DepthZone(
        name = "Midnight Zone",
        startMeters = 1000f,
        description = "Bioluminescent dark",
        color = Color(0xFF7353BA)
    ),
    DepthZone(
        name = "Abyssal Zone",
        startMeters = 3000f,
        description = "Cold black water",
        color = Color(0xFF06D6A0)
    )
)

private val OceanTrivia = listOf(
    "Mercan resifleri okyanusun en kalabalık yaşam alanlarından biridir.",
    "Bazı derin deniz canlıları kendi ışığını üreterek av bulur.",
    "Küçük balık sürüleri birlikte yüzerek avcılara karşı daha güvende kalır.",
    "Deniz çayırları genç balıklar için doğal saklanma alanı oluşturur.",
    "Köpek balıkları sudaki titreşimleri çok hassas biçimde algılayabilir.",
    "Twilight zone bölgesinde ışık azalır, ama birçok canlı gece yüzeye göç eder.",
    "Abyssal bölgede besin az olduğu için canlılar enerjiyi çok dikkatli kullanır.",
    "Planktonlar birçok balığın ve deniz canlısının besin zincirindeki ilk halkadır.",
    "Kelp ormanları balıklar için hem yiyecek hem de sığınak sağlar.",
    "Derinlik arttıkça basınç yükselir ve rahatlık hızla etkilenebilir."
)

private val DepthDecorations = listOf(
    DepthDecoration(
        id = "shallow_grass_left",
        depthMeters = 70f,
        xFraction = 0.08f,
        imageRes = R.drawable.deco_seaweed_grass_a,
        widthDp = 96f,
        alpha = 0.92f,
        parallax = 1f
    ),
    DepthDecoration(
        id = "shallow_green_mid",
        depthMeters = 95f,
        xFraction = 0.34f,
        imageRes = R.drawable.deco_seaweed_green_a,
        widthDp = 118f,
        alpha = 0.9f,
        parallax = 1f
    ),
    DepthDecoration(
        id = "shallow_coral_right",
        depthMeters = 130f,
        xFraction = 0.78f,
        imageRes = R.drawable.deco_seaweed_pink_a,
        widthDp = 112f,
        alpha = 0.88f,
        parallax = 1f
    ),
    DepthDecoration(
        id = "shallow_orange",
        depthMeters = 170f,
        xFraction = 0.56f,
        imageRes = R.drawable.deco_seaweed_orange_b,
        widthDp = 100f,
        alpha = 0.86f,
        parallax = 1f
    ),
    DepthDecoration(
        id = "rock_shelf_left",
        depthMeters = 260f,
        xFraction = 0.18f,
        imageRes = R.drawable.deco_rock_a,
        widthDp = 118f,
        alpha = 0.82f,
        parallax = 1f
    ),
    DepthDecoration(
        id = "twilight_bg_weed",
        depthMeters = 520f,
        xFraction = 0.7f,
        imageRes = R.drawable.deco_background_seaweed_a,
        widthDp = 120f,
        alpha = 0.44f,
        parallax = 0.72f
    ),
    DepthDecoration(
        id = "twilight_weed_left",
        depthMeters = 820f,
        xFraction = 0.22f,
        imageRes = R.drawable.deco_background_seaweed_e,
        widthDp = 126f,
        alpha = 0.42f,
        parallax = 0.7f
    ),
    DepthDecoration(
        id = "twilight_rock_right",
        depthMeters = 1060f,
        xFraction = 0.82f,
        imageRes = R.drawable.deco_background_rock_a,
        widthDp = 150f,
        alpha = 0.45f,
        parallax = 0.68f
    ),
    DepthDecoration(
        id = "midnight_bg_weed",
        depthMeters = 1500f,
        xFraction = 0.14f,
        imageRes = R.drawable.deco_background_seaweed_a,
        widthDp = 112f,
        alpha = 0.32f,
        parallax = 0.62f
    ),
    DepthDecoration(
        id = "midnight_rock",
        depthMeters = 2080f,
        xFraction = 0.62f,
        imageRes = R.drawable.deco_background_rock_b,
        widthDp = 170f,
        alpha = 0.34f,
        parallax = 0.58f
    ),
    DepthDecoration(
        id = "abyss_weed",
        depthMeters = 3180f,
        xFraction = 0.28f,
        imageRes = R.drawable.deco_background_seaweed_e,
        widthDp = 110f,
        alpha = 0.28f,
        parallax = 0.52f
    ),
    DepthDecoration(
        id = "abyss_rock",
        depthMeters = 3720f,
        xFraction = 0.74f,
        imageRes = R.drawable.deco_rock_b,
        widthDp = 140f,
        alpha = 0.5f,
        parallax = 0.86f
    )
)

private val AmbientCreatures = listOf(
    AmbientCreature(
        name = "Clown Reef Fish",
        depthMeters = 45f,
        imageRes = R.drawable.fish_sunburst,
        sizeDp = 62f,
        speed = 0.34f,
        phase = 0.08f,
        swimsRight = true,
        verticalDriftPx = 12f
    ),
    AmbientCreature(
        name = "Rosefin",
        depthMeters = 80f,
        imageRes = R.drawable.fish_rosefin,
        sizeDp = 58f,
        speed = 0.42f,
        phase = 0.52f,
        swimsRight = false,
        verticalDriftPx = 16f
    ),
    AmbientCreature(
        name = "Reef Guardian",
        depthMeters = 135f,
        imageRes = R.drawable.fish_reef_guardian,
        sizeDp = 76f,
        speed = 0.25f,
        phase = 0.31f,
        swimsRight = true,
        verticalDriftPx = 10f
    ),
    AmbientCreature(
        name = "Silver Needle",
        depthMeters = 230f,
        imageRes = R.drawable.fish_silver_needle,
        sizeDp = 48f,
        speed = 0.62f,
        phase = 0.12f,
        swimsRight = true,
        verticalDriftPx = 18f
    ),
    AmbientCreature(
        name = "Yellowfin Ace",
        depthMeters = 360f,
        imageRes = R.drawable.fish_yellowfin_ace,
        sizeDp = 70f,
        speed = 0.5f,
        phase = 0.72f,
        swimsRight = false,
        verticalDriftPx = 20f
    ),
    AmbientCreature(
        name = "Aqua Shark",
        depthMeters = 1180f,
        imageRes = R.drawable.fish_aqua_shark,
        sizeDp = 104f,
        speed = 0.33f,
        phase = 0.4f,
        swimsRight = true,
        verticalDriftPx = 14f
    ),
    AmbientCreature(
        name = "Moon Glider",
        depthMeters = 760f,
        imageRes = R.drawable.fish_moon_glider,
        sizeDp = 94f,
        speed = 0.22f,
        phase = 0.03f,
        swimsRight = false,
        verticalDriftPx = 26f
    ),
    AmbientCreature(
        name = "Violet Razor",
        depthMeters = 1080f,
        imageRes = R.drawable.fish_violet_razor,
        sizeDp = 68f,
        speed = 0.46f,
        phase = 0.66f,
        swimsRight = true,
        verticalDriftPx = 22f
    ),
    AmbientCreature(
        name = "Lantern Dart",
        depthMeters = 1380f,
        imageRes = R.drawable.fish_lantern_dart,
        sizeDp = 78f,
        speed = 0.39f,
        phase = 0.21f,
        swimsRight = false,
        verticalDriftPx = 28f
    ),
    AmbientCreature(
        name = "Crimson Glider",
        depthMeters = 1720f,
        imageRes = R.drawable.fish_crimson_glider,
        sizeDp = 72f,
        speed = 0.36f,
        phase = 0.87f,
        swimsRight = true,
        verticalDriftPx = 20f
    ),
    AmbientCreature(
        name = "Deep Aqua Shark",
        depthMeters = 1880f,
        imageRes = R.drawable.fish_aqua_shark,
        sizeDp = 112f,
        speed = 0.36f,
        phase = 0.77f,
        swimsRight = false,
        verticalDriftPx = 18f
    ),
    AmbientCreature(
        name = "Stone Shark",
        depthMeters = 2200f,
        imageRes = R.drawable.fish_stone_shark,
        sizeDp = 118f,
        speed = 0.24f,
        phase = 0.58f,
        swimsRight = false,
        verticalDriftPx = 16f
    ),
    AmbientCreature(
        name = "Ancient Grouper",
        depthMeters = 2650f,
        imageRes = R.drawable.fish_ancient_grouper,
        sizeDp = 112f,
        speed = 0.2f,
        phase = 0.15f,
        swimsRight = true,
        verticalDriftPx = 12f
    ),
    AmbientCreature(
        name = "Turquoise Tank",
        depthMeters = 3180f,
        imageRes = R.drawable.fish_turquoise_tank,
        sizeDp = 96f,
        speed = 0.19f,
        phase = 0.69f,
        swimsRight = false,
        verticalDriftPx = 18f
    ),
    AmbientCreature(
        name = "Abyss Stone Shark",
        depthMeters = 3320f,
        imageRes = R.drawable.fish_stone_shark,
        sizeDp = 128f,
        speed = 0.22f,
        phase = 0.26f,
        swimsRight = true,
        verticalDriftPx = 14f
    ),
    AmbientCreature(
        name = "Deep Royal Snapper",
        depthMeters = 3550f,
        imageRes = R.drawable.fish_royal_snapper,
        sizeDp = 84f,
        speed = 0.28f,
        phase = 0.38f,
        swimsRight = true,
        verticalDriftPx = 24f
    ),
    AmbientCreature(
        name = "Abyss Grouper",
        depthMeters = 3740f,
        imageRes = R.drawable.fish_ancient_grouper,
        sizeDp = 122f,
        speed = 0.18f,
        phase = 0.49f,
        swimsRight = false,
        verticalDriftPx = 12f
    ),
    AmbientCreature(
        name = "Icefin",
        depthMeters = 3860f,
        imageRes = R.drawable.fish_icefin,
        sizeDp = 76f,
        speed = 0.31f,
        phase = 0.9f,
        swimsRight = false,
        verticalDriftPx = 18f
    )
)

private val FishRoster = listOf(
    PlayableFish(
        id = "aqua_shark",
        name = "Aqua Shark",
        habitat = "Open Ocean",
        personality = "Swift hunter",
        imageRes = R.drawable.fish_aqua_shark,
        accent = Color(0xFF00D5FF),
        speedPxPerSecond = 500f,
        agility = 0.76f,
        stamina = 0.64f
    ),
    PlayableFish(
        id = "ancient_grouper",
        name = "Ancient Grouper",
        habitat = "Coastal Caves",
        personality = "Armored tank",
        imageRes = R.drawable.fish_ancient_grouper,
        accent = Color(0xFFB8D0CF),
        speedPxPerSecond = 260f,
        agility = 0.42f,
        stamina = 0.97f
    ),
    PlayableFish(
        id = "rosefin",
        name = "Rosefin",
        habitat = "Coral Reef",
        personality = "Nimble dazzler",
        imageRes = R.drawable.fish_rosefin,
        accent = Color(0xFFFF7AA2),
        speedPxPerSecond = 340f,
        agility = 0.78f,
        stamina = 0.66f
    ),
    PlayableFish(
        id = "sunburst",
        name = "Sunburst",
        habitat = "Coral Reef",
        personality = "Balanced explorer",
        imageRes = R.drawable.fish_sunburst,
        accent = Color(0xFFFFD166),
        speedPxPerSecond = 360f,
        agility = 0.74f,
        stamina = 0.68f
    ),
    PlayableFish(
        id = "prism_tailer",
        name = "Prism Tailer",
        habitat = "Lagoon Edge",
        personality = "Flashy sprinter",
        imageRes = R.drawable.fish_prism_tailer,
        accent = Color(0xFFB388FF),
        speedPxPerSecond = 430f,
        agility = 0.86f,
        stamina = 0.55f
    ),
    PlayableFish(
        id = "silver_needle",
        name = "Silver Needle",
        habitat = "Open Ocean",
        personality = "Tiny speedster",
        imageRes = R.drawable.fish_silver_needle,
        accent = Color(0xFFDCE9F2),
        speedPxPerSecond = 520f,
        agility = 0.88f,
        stamina = 0.42f
    ),
    PlayableFish(
        id = "yellowfin_ace",
        name = "Yellowfin Ace",
        habitat = "Blue Current",
        personality = "Long-distance racer",
        imageRes = R.drawable.fish_yellowfin_ace,
        accent = Color(0xFFF7E733),
        speedPxPerSecond = 500f,
        agility = 0.79f,
        stamina = 0.61f
    ),
    PlayableFish(
        id = "moon_glider",
        name = "Moon Glider",
        habitat = "Deep Reef",
        personality = "Calm drifter",
        imageRes = R.drawable.fish_moon_glider,
        accent = Color(0xFF8EECF5),
        speedPxPerSecond = 300f,
        agility = 0.58f,
        stamina = 0.8f
    ),
    PlayableFish(
        id = "blue_sprinter",
        name = "Blue Sprinter",
        habitat = "Open Ocean",
        personality = "Fast striker",
        imageRes = R.drawable.fish_blue_sprinter,
        accent = Color(0xFF4CC9F0),
        speedPxPerSecond = 470f,
        agility = 0.82f,
        stamina = 0.58f
    ),
    PlayableFish(
        id = "ember_bloom",
        name = "Ember Bloom",
        habitat = "Warm Reef",
        personality = "Balanced turner",
        imageRes = R.drawable.fish_ember_bloom,
        accent = Color(0xFFFF9F1C),
        speedPxPerSecond = 360f,
        agility = 0.7f,
        stamina = 0.74f
    ),
    PlayableFish(
        id = "reef_guardian",
        name = "Reef Guardian",
        habitat = "Tropical Shelf",
        personality = "Stable survivor",
        imageRes = R.drawable.fish_reef_guardian,
        accent = Color(0xFF80ED99),
        speedPxPerSecond = 315f,
        agility = 0.62f,
        stamina = 0.88f
    ),
    PlayableFish(
        id = "icefin",
        name = "Icefin",
        habitat = "Cold Current",
        personality = "Precise cruiser",
        imageRes = R.drawable.fish_icefin,
        accent = Color(0xFF90DBF4),
        speedPxPerSecond = 410f,
        agility = 0.72f,
        stamina = 0.68f
    ),
    PlayableFish(
        id = "kelp_mender",
        name = "Kelp Mender",
        habitat = "Kelp Forest",
        personality = "Careful survivor",
        imageRes = R.drawable.fish_kelp_mender,
        accent = Color(0xFFA7C957),
        speedPxPerSecond = 290f,
        agility = 0.6f,
        stamina = 0.9f
    ),
    PlayableFish(
        id = "violet_razor",
        name = "Violet Razor",
        habitat = "Twilight Zone",
        personality = "Agile predator",
        imageRes = R.drawable.fish_violet_razor,
        accent = Color(0xFFC77DFF),
        speedPxPerSecond = 485f,
        agility = 0.88f,
        stamina = 0.5f
    ),
    PlayableFish(
        id = "crimson_glider",
        name = "Crimson Glider",
        habitat = "Rocky Reef",
        personality = "Sharp turner",
        imageRes = R.drawable.fish_crimson_glider,
        accent = Color(0xFFFF6B6B),
        speedPxPerSecond = 395f,
        agility = 0.9f,
        stamina = 0.52f
    ),
    PlayableFish(
        id = "tide_striper",
        name = "Tide Striper",
        habitat = "Tidal Rocks",
        personality = "Steady scout",
        imageRes = R.drawable.fish_tide_striper,
        accent = Color(0xFFFF8FA3),
        speedPxPerSecond = 360f,
        agility = 0.66f,
        stamina = 0.76f
    ),
    PlayableFish(
        id = "goldfin_racer",
        name = "Goldfin Racer",
        habitat = "Sunlit Reef",
        personality = "Quick dodger",
        imageRes = R.drawable.fish_goldfin_racer,
        accent = Color(0xFFFFD60A),
        speedPxPerSecond = 420f,
        agility = 0.84f,
        stamina = 0.62f
    ),
    PlayableFish(
        id = "lantern_dart",
        name = "Lantern Dart",
        habitat = "Night Reef",
        personality = "Lightning turner",
        imageRes = R.drawable.fish_lantern_dart,
        accent = Color(0xFF06D6A0),
        speedPxPerSecond = 455f,
        agility = 0.91f,
        stamina = 0.48f
    ),
    PlayableFish(
        id = "royal_snapper",
        name = "Royal Snapper",
        habitat = "Coral Garden",
        personality = "Sturdy brawler",
        imageRes = R.drawable.fish_royal_snapper,
        accent = Color(0xFFFFB703),
        speedPxPerSecond = 330f,
        agility = 0.64f,
        stamina = 0.88f
    ),
    PlayableFish(
        id = "stone_shark",
        name = "Stone Shark",
        habitat = "Deep Coast",
        personality = "Heavy cruiser",
        imageRes = R.drawable.fish_stone_shark,
        accent = Color(0xFFB8C0C2),
        speedPxPerSecond = 285f,
        agility = 0.46f,
        stamina = 0.96f
    ),
    PlayableFish(
        id = "turquoise_tank",
        name = "Turquoise Tank",
        habitat = "Seagrass Bed",
        personality = "Gentle bruiser",
        imageRes = R.drawable.fish_turquoise_tank,
        accent = Color(0xFF64DFDF),
        speedPxPerSecond = 305f,
        agility = 0.56f,
        stamina = 0.92f
    )
)

@Composable
fun SeaverseGameApp() {
    var selectedFish by remember { mutableStateOf<PlayableFish?>(null) }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        selectedFish?.let { fish ->
            OceanGameScreen(
                fish = fish,
                onChangeFish = { selectedFish = null }
            )
        } ?: FishOnboardingScreen(
            fishRoster = FishRoster,
            onFishSelected = { selectedFish = it }
        )
    }
}

@Composable
private fun FishOnboardingScreen(
    fishRoster: List<PlayableFish>,
    onFishSelected: (PlayableFish) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OceanBackdrop(dimAmount = 0.42f)
        UnderwaterOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Choose Your Fish",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Pick a swimmer and dive straight into SeaVerse.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 172.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(fishRoster, key = { it.id }) { fish ->
                    FishSelectionCard(
                        fish = fish,
                        onClick = { onFishSelected(fish) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FishSelectionCard(
    fish: PlayableFish,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.84f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC061927)
        ),
        border = BorderStroke(1.dp, fish.accent.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                fish.accent.copy(alpha = 0.28f),
                                Color(0x3310C7D8),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(fish.imageRes),
                    contentDescription = fish.name,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .graphicsLayer(scaleX = -1f),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = fish.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${fish.habitat} - ${fish.personality}",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FishStat(label = "Speed", value = fish.speedPxPerSecond / 500f, color = fish.accent)
                FishStat(label = "Agility", value = fish.agility, color = Color(0xFF7BDFF2))
                FishStat(label = "Stamina", value = fish.stamina, color = Color(0xFFA7F3D0))
            }
        }
    }
}

@Composable
private fun FishStat(
    label: String,
    value: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(52.dp),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .height(6.dp)
                .weight(1f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun OceanGameScreen(
    fish: PlayableFish,
    onChangeFish: () -> Unit
) {
    var sceneSize by remember { mutableStateOf(IntSize.Zero) }
    var joystickVector by remember { mutableStateOf(Offset.Zero) }
    val currentJoystickVector by rememberUpdatedState(joystickVector)
    var playerPosition by remember(fish.id) { mutableStateOf<Offset?>(null) }
    var facingRight by remember(fish.id) { mutableStateOf(true) }
    var selectedFishInfo by remember { mutableStateOf<FishInfo?>(null) }
    var activeChatInfo by remember { mutableStateOf<FishInfo?>(null) }
    val startingScenario = remember(fish.id) { startingScenarioFor(fish) }
    var activeInfoCard by remember(fish.id) {
        mutableStateOf<LearningInfoCard?>(
            LearningInfoCard(
                title = "Başlangıç Bilgisi",
                message = startingScenario,
                type = EventLogType.Info
            )
        )
    }
    var health by remember(fish.id) { mutableStateOf(100f) }
    var comfort by remember(fish.id) { mutableStateOf(100f) }
    var hunger by remember(fish.id) { mutableStateOf(50) }
    var energy by remember(fish.id) { mutableStateOf(100) }
    var score by remember(fish.id) { mutableStateOf(0) }
    var survivedSeconds by remember(fish.id) { mutableStateOf(0) }
    var lastEventAtSeconds by remember(fish.id) { mutableStateOf(0) }
    var activeEvent by remember(fish.id) { mutableStateOf<Event?>(null) }
    var eventFeedback by remember(fish.id) { mutableStateOf<String?>(null) }
    var isEventLoading by remember(fish.id) { mutableStateOf(false) }
    var pendingEventRequest by remember(fish.id) { mutableStateOf<GameState?>(null) }
    var askedEventTexts by remember(fish.id) { mutableStateOf<List<String>>(emptyList()) }
    var choiceHistory by remember(fish.id) { mutableStateOf<List<ChoiceRecord>>(emptyList()) }
    var consumedAmbientCreatures by remember(fish.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var eatenPreyCount by remember(fish.id) { mutableStateOf(0) }
    var lastPredatorHitAt by remember(fish.id) { mutableStateOf(-10) }
    var eventLog by remember(fish.id) {
        mutableStateOf(
            listOf(
                EventLogEntry(
                    type = EventLogType.Info,
                    message = "${fish.name} türü olarak oyuna başladın."
                ),
                EventLogEntry(
                    type = EventLogType.Extra,
                    message = startingScenario
                )
            )
        )
    }
    var hasShownDepthWarning by remember(fish.id) { mutableStateOf(false) }
    var wasOutsideComfortDepth by remember(fish.id) { mutableStateOf(false) }
    var lastHealthWarningAt by remember(fish.id) { mutableStateOf(-10) }
    var lastTriviaAt by remember(fish.id) { mutableStateOf(0) }
    var hasLoggedGameOver by remember(fish.id) { mutableStateOf(false) }

    fun appendEventLog(type: EventLogType, vararg messages: String) {
        val entries = messages.map { message ->
            EventLogEntry(type = type, message = message)
        }
        eventLog = (eventLog + entries).takeLast(18)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sceneSize = it }
    ) {
        val density = LocalDensity.current
        val baseFishSize = if (maxWidth < 680.dp) 86.dp else 118.dp
        val playerGrowthScale = (1f + eatenPreyCount * 0.055f).coerceAtMost(1.55f)
        val fishSize = (baseFishSize.value * playerGrowthScale).dp
        val fishSizePx = with(density) { fishSize.toPx() }
        val hiddenCreatureNames = consumedAmbientCreatures
            .filterValues { respawnAtSecond -> respawnAtSecond > survivedSeconds }
            .keys
        val worldWidthPx = (sceneSize.width * WorldWidthScreens)
            .coerceAtLeast(sceneSize.width.toFloat())
        val worldHeightPx = (sceneSize.height * WorldDepthScreens)
            .coerceAtLeast(sceneSize.height.toFloat())
        val comfortRange = comfortRangeFor(fish)

        LaunchedEffect(sceneSize, fish.id, fishSizePx, worldWidthPx, worldHeightPx) {
            if (sceneSize.width == 0 || sceneSize.height == 0) return@LaunchedEffect

            fun initialPosition() = Offset(
                x = sceneSize.width * 0.44f,
                y = sceneSize.height * 0.42f
            )

            fun clampPlayerPosition(position: Offset): Offset {
                val halfFish = fishSizePx / 2f
                val maxX = (worldWidthPx - halfFish).coerceAtLeast(halfFish)
                val maxY = (worldHeightPx - halfFish).coerceAtLeast(halfFish)
                return Offset(
                    x = position.x.coerceIn(halfFish, maxX),
                    y = position.y.coerceIn(halfFish, maxY)
                )
            }

            playerPosition = clampPlayerPosition(playerPosition ?: initialPosition())

            var previousFrameNanos = 0L
            var secondAccumulator = 0f
            while (isActive) {
                withFrameNanos { frameNanos ->
                    if (previousFrameNanos != 0L && health > 0f) {
                        val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                        val safeDeltaSeconds = deltaSeconds.coerceAtMost(0.05f)
                        val input = currentJoystickVector
                        val deadZone = 0.05f
                        var current = playerPosition ?: initialPosition()

                        if (input.getDistance() > deadZone) {
                            val energySpeedFactor = 0.58f + (energy / 100f) * 0.42f
                            val speed = fish.speedPxPerSecond *
                                (0.78f + fish.agility * 0.22f) *
                                energySpeedFactor
                            val next = Offset(
                                x = current.x + input.x * speed * safeDeltaSeconds,
                                y = current.y + input.y * speed * safeDeltaSeconds
                            )
                            current = clampPlayerPosition(next)
                            playerPosition = current
                            if (input.x > 0.08f) facingRight = true
                            if (input.x < -0.08f) facingRight = false
                        }

                        val currentDepthMeters = depthMetersForPosition(
                            positionY = current.y,
                            fishSizePx = fishSizePx,
                            worldHeightPx = worldHeightPx
                        )
                        val overDepth = (currentDepthMeters - comfortRange.maxComfortDepthMeters)
                            .coerceAtLeast(0f)

                        comfort = if (overDepth > 0f) {
                            (comfort - 0.5f * safeDeltaSeconds).coerceIn(0f, 100f)
                        } else {
                            (comfort + (8f + fish.stamina * 8f) * safeDeltaSeconds)
                                .coerceIn(0f, 100f)
                        }

                        health = when {
                            comfort <= 0f -> {
                                val damage = 1.2f + overDepth / 900f
                                (health - damage * safeDeltaSeconds).coerceIn(0f, 100f)
                            }

                            comfort < 25f && overDepth > 0f -> {
                                val damage = 0.35f + overDepth / 1400f
                                (health - damage * safeDeltaSeconds).coerceIn(0f, 100f)
                            }

                            comfort > 82f && overDepth == 0f -> {
                                (health + 2.2f * safeDeltaSeconds).coerceIn(0f, 100f)
                            }

                            else -> health
                        }

                        secondAccumulator += safeDeltaSeconds
                        while (secondAccumulator >= 1f) {
                            secondAccumulator -= 1f
                            val nextSecond = survivedSeconds + 1
                            survivedSeconds = nextSecond
                            score += 1

                            if (nextSecond % 5 == 0) {
                                hunger = (hunger + 1).coerceIn(0, 100)
                            }

                            if (nextSecond % 2 == 0) {
                                if (input.getDistance() > deadZone) {
                                    energy = (energy - 1).coerceIn(0, 100)
                                } else {
                                    energy = (energy + 1).coerceIn(0, 100)
                                }
                            }

                            if (hunger > 92 && nextSecond % 3 == 0) {
                                health = (health - 1f).coerceIn(0f, 100f)
                            }
                        }
                    }
                    previousFrameNanos = frameNanos
                }
            }
        }

        val position = playerPosition ?: Offset(
            x = sceneSize.width * 0.44f,
            y = sceneSize.height * 0.42f
        )
        val cameraX = if (sceneSize.width > 0) {
            (position.x - sceneSize.width * 0.48f)
                .coerceIn(0f, (worldWidthPx - sceneSize.width).coerceAtLeast(0f))
        } else {
            0f
        }
        val cameraY = if (sceneSize.height > 0) {
            (position.y - sceneSize.height * 0.48f)
                .coerceIn(0f, (worldHeightPx - sceneSize.height).coerceAtLeast(0f))
        } else {
            0f
        }
        val depthMeters = if (worldHeightPx > fishSizePx) {
            ((position.y - fishSizePx / 2f) / (worldHeightPx - fishSizePx))
                .coerceIn(0f, 1f) * MaxDiveDepthMeters
        } else {
            0f
        }
        val depthZone = depthZoneFor(depthMeters)
        val depthOverComfort = (depthMeters - comfortRange.maxComfortDepthMeters)
            .coerceAtLeast(0f)
        val gameSnapshot = GameState(
            fishId = fish.id,
            health = health.roundToInt().coerceIn(0, 100),
            comfort = comfort.roundToInt().coerceIn(0, 100),
            hunger = hunger,
            energy = energy,
            depth = depthMeters.roundToInt(),
            score = score,
            zone = GameEventEngine.zoneForDepth(depthMeters.roundToInt()),
            survivedSeconds = survivedSeconds,
            lastEventAtSeconds = lastEventAtSeconds
        )
        val isGameOver = gameSnapshot.health <= 0

        LaunchedEffect(isGameOver) {
            if (isGameOver && !hasLoggedGameOver) {
                activeEvent = null
                pendingEventRequest = null
                joystickVector = Offset.Zero
                appendEventLog(
                    EventLogType.Warning,
                    "Sağlık sıfırlandı. Seçim özeti açıldı."
                )
                hasLoggedGameOver = true
            }
        }

        LaunchedEffect(eventFeedback) {
            if (eventFeedback != null) {
                delay(4800)
                eventFeedback = null
            }
        }

        LaunchedEffect(survivedSeconds, consumedAmbientCreatures) {
            if (consumedAmbientCreatures.any { (_, respawnAtSecond) ->
                    respawnAtSecond <= survivedSeconds
                }
            ) {
                consumedAmbientCreatures = consumedAmbientCreatures
                    .filterValues { respawnAtSecond -> respawnAtSecond > survivedSeconds }
            }
        }

        LaunchedEffect(
            gameSnapshot.survivedSeconds,
            gameSnapshot.depth,
            gameSnapshot.comfort,
            gameSnapshot.health
        ) {
            if (isGameOver) return@LaunchedEffect

            if (depthOverComfort > 0f && !hasShownDepthWarning) {
                appendEventLog(
                    EventLogType.Warning,
                    "Derinlik arttı: ${gameSnapshot.depth} m. Rahatlık düşüyor."
                )
                hasShownDepthWarning = true
            }

            if (depthOverComfort > 0f) {
                wasOutsideComfortDepth = true
            } else if (wasOutsideComfortDepth) {
                appendEventLog(
                    EventLogType.Ideal,
                    "İdeal derinlik seviyesindesin. Rahatlık toparlanıyor."
                )
                wasOutsideComfortDepth = false
            }

            if (depthOverComfort > 0f &&
                gameSnapshot.comfort < 25 &&
                gameSnapshot.survivedSeconds - lastHealthWarningAt >= 8
            ) {
                appendEventLog(
                    EventLogType.Warning,
                    "Konfor çok düşük; sağlık azalmaya başlayabilir."
                )
                lastHealthWarningAt = gameSnapshot.survivedSeconds
            }
        }

        LaunchedEffect(gameSnapshot.survivedSeconds) {
            if (isGameOver) return@LaunchedEffect

            if (gameSnapshot.survivedSeconds >= 20 &&
                gameSnapshot.survivedSeconds - lastTriviaAt >= 30
            ) {
                val trivia = OceanTrivia.random()
                appendEventLog(EventLogType.Extra, trivia)
                if (activeInfoCard == null) {
                    activeInfoCard = LearningInfoCard(
                        title = "Ekstra Bilgi",
                        message = trivia,
                        type = EventLogType.Extra
                    )
                }
                lastTriviaAt = gameSnapshot.survivedSeconds
            }
        }

        LaunchedEffect(
            gameSnapshot.survivedSeconds,
            gameSnapshot.hunger,
            gameSnapshot.energy,
            gameSnapshot.zone,
            selectedFishInfo,
            activeChatInfo,
            activeInfoCard,
            eventFeedback
        ) {
            if (isGameOver) return@LaunchedEffect
            if (pendingEventRequest != null) return@LaunchedEffect
            val canTrigger = GameEventEngine.shouldTriggerEvent(
                state = gameSnapshot,
                elapsedSec = gameSnapshot.survivedSeconds,
                hasActiveEvent = activeEvent != null ||
                    selectedFishInfo != null ||
                    activeChatInfo != null ||
                    activeInfoCard != null ||
                    eventFeedback != null,
                isLoading = isEventLoading
            )
            if (!canTrigger) return@LaunchedEffect

            pendingEventRequest = gameSnapshot
            lastEventAtSeconds = gameSnapshot.survivedSeconds
            appendEventLog(EventLogType.Info, "Yeni bir olay geliyor, hazırlanın.")
        }

        LaunchedEffect(
            pendingEventRequest?.fishId,
            pendingEventRequest?.survivedSeconds,
            pendingEventRequest?.zone
        ) {
            val requestState = pendingEventRequest ?: return@LaunchedEffect
            if (isGameOver) return@LaunchedEffect

            isEventLoading = true
            try {
                if (activeEvent != null) return@LaunchedEffect

                val info = fish.toFishInfo(requestState.depth.toFloat())
                val context = FishEventContext(
                    fishId = fish.id,
                    fishName = fish.name,
                    habitat = fish.habitat,
                    personality = fish.personality,
                    depthRange = info.depthRange,
                    dietType = info.dietType,
                    food = info.food,
                    ecologicalRole = info.ecologicalRole,
                    gameState = requestState,
                    previousEventTexts = askedEventTexts
                )
                val event = OpenAiFishEventClient
                    .generateEvent(context)
                    .getOrElse {
                        GameEventEngine.fallbackEvent(
                            state = requestState,
                            fishName = fish.name,
                            habitat = fish.habitat,
                            dietType = info.dietType
                        )
                    }

                activeEvent = event
                askedEventTexts = (askedEventTexts + event.text).takeLast(12)
                appendEventLog(EventLogType.Info, event.text)
            } finally {
                isEventLoading = false
                pendingEventRequest = null
            }
        }

        DepthOceanWorld(
            cameraX = cameraX,
            cameraY = cameraY,
            worldWidthPx = worldWidthPx,
            worldHeightPx = worldHeightPx,
            depthMeters = depthMeters,
            modifier = Modifier.fillMaxSize()
        )

        DepthDecorationsLayer(
            cameraX = cameraX,
            cameraY = cameraY,
            worldWidthPx = worldWidthPx,
            worldHeightPx = worldHeightPx,
            modifier = Modifier.fillMaxSize()
        )

        AmbientCreaturesLayer(
            cameraX = cameraX,
            cameraY = cameraY,
            worldWidthPx = worldWidthPx,
            worldHeightPx = worldHeightPx,
            playerPosition = position,
            playerSizeDp = fishSize.value,
            playerSizePx = fishSizePx,
            consumedCreatureNames = hiddenCreatureNames,
            interactionTick = survivedSeconds / 3,
            modifier = Modifier.fillMaxSize(),
            onCreatureClick = { selectedFishInfo = it },
            onPreyEaten = { creature ->
                if (!isGameOver && creature.name !in hiddenCreatureNames) {
                    val nextEatenPreyCount = eatenPreyCount + 1
                    val respawnDelaySeconds = preyRespawnDelaySeconds(creature.name)
                    consumedAmbientCreatures = consumedAmbientCreatures +
                        (creature.name to survivedSeconds + respawnDelaySeconds)
                    eatenPreyCount = nextEatenPreyCount
                    hunger = (hunger - 18).coerceIn(0, 100)
                    energy = (energy + 6).coerceIn(0, 100)
                    health = (health + 2f).coerceIn(0f, 100f)
                    score += 18
                    appendEventLog(
                        EventLogType.Info,
                        "Küçük ${creature.name} yakalandı. +18 puan, açlık azaldı, biraz büyüdün. ${respawnDelaySeconds} saniye sonra yeniden görünebilir."
                    )
                }
            },
            onPredatorHit = { creature ->
                if (!isGameOver && survivedSeconds - lastPredatorHitAt >= 3) {
                    lastPredatorHitAt = survivedSeconds
                    health = (health - 16f).coerceIn(0f, 100f)
                    energy = (energy - 10).coerceIn(0, 100)
                    score = (score - 8).coerceAtLeast(0)
                    appendEventLog(
                        EventLogType.Warning,
                        "Büyük ${creature.name} saldırdı. Kaç; sağlık ve enerji düştü."
                    )
                }
            }
        )

        Image(
            painter = painterResource(fish.imageRes),
            contentDescription = fish.name,
            modifier = Modifier
                .size(fishSize)
                .offset {
                    IntOffset(
                        x = (position.x - cameraX - fishSizePx / 2f).roundToInt(),
                        y = (position.y - cameraY - fishSizePx / 2f).roundToInt()
                    )
                }
                .clickable {
                    selectedFishInfo = fish.toFishInfo(depthMeters)
                }
                .graphicsLayer {
                    scaleX = if (facingRight) -1f else 1f
                    rotationZ = (currentJoystickVector.y * 7f).coerceIn(-7f, 7f)
                },
            contentScale = ContentScale.Fit
        )

        PlayerHud(
            fish = fish,
            onChangeFish = onChangeFish,
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(16.dp)
        )

        VitalStatusHud(
            health = health,
            comfort = comfort,
            hunger = hunger,
            energy = energy,
            score = score,
            comfortRange = comfortRange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(16.dp)
        )

        DepthGauge(
            depthMeters = depthMeters,
            depthZone = depthZone,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .systemBarsPadding()
                .padding(start = 6.dp)
        )

        MovementJoystick(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(24.dp),
            accent = fish.accent,
            onVectorChange = { joystickVector = it }
        )

        activeEvent?.takeUnless { isGameOver }?.let { event ->
            FishEventCard(
                event = event,
                depthZone = depthZone,
                accent = fish.accent,
                onOptionSelected = { option ->
                    val result = GameEventEngine.applyOption(gameSnapshot, option)
                    health = result.state.health.toFloat()
                    comfort = result.state.comfort.toFloat()
                    hunger = result.state.hunger
                    energy = result.state.energy
                    score = result.state.score
                    choiceHistory = (
                        choiceHistory + ChoiceRecord(
                            eventText = event.text,
                            optionText = option.text,
                            feedback = result.feedback,
                            assessment = result.assessment,
                            scoreDelta = result.state.score - gameSnapshot.score,
                            healthDelta = result.state.health - gameSnapshot.health,
                            comfortDelta = result.state.comfort - gameSnapshot.comfort,
                            hungerDelta = result.state.hunger - gameSnapshot.hunger,
                            energyDelta = result.state.energy - gameSnapshot.energy,
                            depthMeters = gameSnapshot.depth
                        )
                    ).takeLast(12)
                    activeEvent = null
                    eventFeedback = result.feedback
                    activeInfoCard = LearningInfoCard(
                        title = "${result.assessment.label} Seçim",
                        message = result.feedback,
                        type = when (result.assessment) {
                            ChoiceAssessment.Correct -> EventLogType.Ideal
                            ChoiceAssessment.Partial -> EventLogType.Info
                            ChoiceAssessment.Risky -> EventLogType.Warning
                            ChoiceAssessment.Wrong -> EventLogType.Warning
                        }
                    )
                    appendEventLog(
                        EventLogType.Info,
                        "${event.text} karşısında \"${option.text}\" seçimini yaptın. " +
                            "${formatScoreDelta(result.state.score - gameSnapshot.score)} puan."
                    )
                    appendEventLog(
                        EventLogType.Extra,
                        result.feedback
                    )
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .systemBarsPadding()
                    .padding(24.dp)
            )
        }

        EventLogBar(
            messages = eventLog,
            isLoading = isEventLoading && activeEvent == null,
            accent = fish.accent,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .systemBarsPadding()
                .padding(start = 16.dp, bottom = 18.dp)
        )

        selectedFishInfo?.let { info ->
            FishInfoCard(
                info = info,
                onDismiss = { selectedFishInfo = null },
                onChatClick = {
                    activeChatInfo = info
                    selectedFishInfo = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(start = 24.dp, end = 188.dp, bottom = 18.dp)
            )
        }

        activeChatInfo?.let { info ->
            FishChatPanel(
                info = info,
                onDismiss = { activeChatInfo = null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .systemBarsPadding()
                    .padding(24.dp)
            )
        }

        activeInfoCard
            ?.takeUnless {
                activeEvent != null ||
                    selectedFishInfo != null ||
                    activeChatInfo != null ||
                    isGameOver
            }
            ?.let { card ->
                LearningInfoCardView(
                    info = card,
                    accent = fish.accent,
                    onDismiss = { activeInfoCard = null },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .systemBarsPadding()
                        .padding(20.dp)
                )
            }

        if (isGameOver) {
            GameOverSummaryCard(
                fish = fish,
                state = gameSnapshot,
                choices = choiceHistory,
                onChangeFish = onChangeFish,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(18.dp)
            )
        }
    }
}

@Composable
private fun DepthOceanWorld(
    cameraX: Float,
    cameraY: Float,
    worldWidthPx: Float,
    worldHeightPx: Float,
    depthMeters: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "deep ocean ambience")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000),
            repeatMode = RepeatMode.Restart
        ),
        label = "deep particle drift"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "deep light shimmer"
    )
    val shallowBackdropAlpha = (1f - depthMeters / 650f).coerceIn(0f, 0.96f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val safeWorldHeight = worldHeightPx.coerceAtLeast(size.height)
            val topProgress = (cameraY / safeWorldHeight).coerceIn(0f, 1f)
            val middleProgress = ((cameraY + size.height * 0.5f) / safeWorldHeight)
                .coerceIn(0f, 1f)
            val bottomProgress = ((cameraY + size.height) / safeWorldHeight)
                .coerceIn(0f, 1f)

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        oceanColorAt(topProgress),
                        oceanColorAt(middleProgress),
                        oceanColorAt(bottomProgress)
                    )
                )
            )
        }

        Image(
            painter = painterResource(R.drawable.sea_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = shallowBackdropAlpha),
            contentScale = ContentScale.Crop
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val safeWorldWidth = worldWidthPx.coerceAtLeast(size.width)
            val safeWorldHeight = worldHeightPx.coerceAtLeast(size.height)
            val depthProgress = (depthMeters / MaxDiveDepthMeters).coerceIn(0f, 1f)
            val surfaceFade = (1f - cameraY / (safeWorldHeight * 0.22f)).coerceIn(0f, 1f)
            val marineSnowAlpha = (0.08f + depthProgress * 0.22f).coerceIn(0.08f, 0.3f)
            val parallaxX = cameraX * 0.18f

            repeat(7) { index ->
                val startX = size.width * (0.16f + index * 0.11f) - parallaxX % size.width
                val endX = size.width * (0.03f + index * 0.15f + shimmer * 0.04f) -
                    parallaxX % size.width
                drawLine(
                    color = Color.White.copy(alpha = surfaceFade * (0.045f + shimmer * 0.025f)),
                    start = Offset(startX, -20f),
                    end = Offset(endX, size.height * 0.8f),
                    strokeWidth = 30f + index * 7f,
                    cap = StrokeCap.Round
                )
            }

            var markerMeters = 0f
            while (markerMeters <= MaxDiveDepthMeters) {
                val markerY = safeWorldHeight * (markerMeters / MaxDiveDepthMeters) - cameraY
                if (markerY in -8f..(size.height + 8f)) {
                    val strongLine = markerMeters % 1000f == 0f
                    drawLine(
                        color = Color.White.copy(alpha = if (strongLine) 0.18f else 0.08f),
                        start = Offset(0f, markerY),
                        end = Offset(size.width, markerY),
                        strokeWidth = if (strongLine) 2.2f else 1.1f
                    )
                }
                markerMeters += 500f
            }

            repeat(90) { index ->
                val laneWorldX = safeWorldWidth * (((index * 37) % 100) / 100f)
                val wave = sin((drift * 6.28f) + index * 0.8f) * (10f + depthProgress * 18f)
                val x = laneWorldX - cameraX * 0.32f + wave
                val travel = size.height + 160f
                val speed = 0.45f + (index % 7) * 0.08f
                val y = size.height - ((drift * travel * speed + index * 53f + cameraY * 0.12f) % travel)
                val radius = 1.2f + (index % 4) * 0.75f
                if (x in -24f..(size.width + 24f)) {
                    drawCircle(
                        color = Color.White.copy(alpha = marineSnowAlpha),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }

            repeat(16) { index ->
                val worldY = safeWorldHeight * (0.28f + index * 0.042f)
                val y = worldY - cameraY
                if (y in -80f..(size.height + 80f)) {
                    val x = safeWorldWidth * (0.08f + ((index * 29) % 84) / 100f) -
                        cameraX * 0.74f
                    val plantHeight = 28f + (index % 5) * 8f
                    val color = if (index % 2 == 0) Color(0xFF00F5D4) else Color(0xFFC77DFF)
                    if (x in -60f..(size.width + 60f)) {
                        drawLine(
                            color = color.copy(alpha = 0.18f + depthProgress * 0.24f),
                            start = Offset(x, y + plantHeight),
                            end = Offset(x + sin(index.toFloat()) * 14f, y),
                            strokeWidth = 2.4f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = color.copy(alpha = 0.28f + shimmer * 0.18f),
                            radius = 3.8f + (index % 3),
                            center = Offset(x + sin(index.toFloat()) * 14f, y)
                        )
                    }
                }
            }

            val seaFloorTop = safeWorldHeight - size.height * 0.28f
            val floorY = seaFloorTop - cameraY
            if (floorY < size.height + 120f) {
                drawRect(
                    color = Color(0xFF071016),
                    topLeft = Offset(0f, floorY.coerceAtLeast(0f)),
                    size = Size(size.width, size.height - floorY.coerceAtLeast(0f))
                )
                repeat(12) { index ->
                    val rockX = safeWorldWidth * (((index * 19) % 100) / 100f) - cameraX
                    val rockY = floorY + 28f + (index % 4) * 22f
                    if (rockX in -120f..(size.width + 120f)) {
                        drawOval(
                            color = Color(0xFF14242C).copy(alpha = 0.92f),
                            topLeft = Offset(rockX - 42f, rockY),
                            size = Size(84f + (index % 3) * 24f, 26f + (index % 4) * 9f)
                        )
                    }
                }
                repeat(6) { index ->
                    val ventX = safeWorldWidth * (0.12f + index * 0.15f) - cameraX
                    val ventBaseY = floorY + 92f
                    if (ventX in -80f..(size.width + 80f)) {
                        drawLine(
                            color = Color(0xFF00F5D4).copy(alpha = 0.2f + shimmer * 0.15f),
                            start = Offset(ventX, ventBaseY),
                            end = Offset(ventX + sin(index + shimmer) * 18f, ventBaseY - 76f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            drawRect(
                color = Color.Black.copy(alpha = (depthProgress * 0.34f).coerceIn(0f, 0.34f))
            )
        }
    }
}

@Composable
private fun DepthDecorationsLayer(
    cameraX: Float,
    cameraY: Float,
    worldWidthPx: Float,
    worldHeightPx: Float,
    modifier: Modifier = Modifier
) {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { layerSize = it }) {
        val viewportWidth = layerSize.width.toFloat()
        val viewportHeight = layerSize.height.toFloat()
        if (viewportWidth <= 0f || viewportHeight <= 0f) return@Box

        DepthDecorations.forEach { decoration ->
            val worldY = worldYForDepth(decoration.depthMeters, worldHeightPx)
            val parallaxCameraY = cameraY * decoration.parallax
            val parallaxCameraX = cameraX * decoration.parallax
            val worldX = worldWidthPx * decoration.xFraction
            val screenX = worldX - parallaxCameraX
            val screenY = worldY - parallaxCameraY
            val widthPx = with(density) { decoration.widthDp.dp.toPx() }

            if (screenX in -widthPx..(viewportWidth + widthPx) &&
                screenY in -widthPx..(viewportHeight + widthPx)
            ) {
                Image(
                    painter = painterResource(decoration.imageRes),
                    contentDescription = decoration.id,
                    modifier = Modifier
                        .width(decoration.widthDp.dp)
                        .offset {
                            IntOffset(
                                x = (screenX - widthPx / 2f).roundToInt(),
                                y = (screenY - widthPx * 0.55f).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            alpha = decoration.alpha
                            scaleX = if (decoration.xFraction > 0.5f) -1f else 1f
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun ambientDisplaySizeDp(creature: AmbientCreature): Float {
    val name = creature.name
    return when {
        name.contains("Shark", ignoreCase = true) -> creature.sizeDp * 1.68f
        name.contains("Grouper", ignoreCase = true) -> creature.sizeDp * 1.5f
        name.contains("Tank", ignoreCase = true) -> creature.sizeDp * 1.35f
        creature.sizeDp <= 62f -> creature.sizeDp * 0.62f
        creature.sizeDp <= 76f -> creature.sizeDp * 0.76f
        creature.sizeDp <= 86f -> creature.sizeDp * 0.86f
        else -> creature.sizeDp
    }.coerceIn(28f, 206f)
}

private fun ambientCreatureRole(
    creature: AmbientCreature,
    playerSizeDp: Float
): AmbientCreatureRole {
    val displaySize = ambientDisplaySizeDp(creature)
    val name = creature.name
    val canBePredator = creature.depthMeters >= PredatorDepthStartMeters
    return when {
        canBePredator && (
            name.contains("Shark", ignoreCase = true) ||
                name.contains("Grouper", ignoreCase = true) ||
                displaySize >= playerSizeDp * 1.12f
            ) -> AmbientCreatureRole.Predator

        displaySize <= playerSizeDp * 0.74f -> AmbientCreatureRole.Prey
        else -> AmbientCreatureRole.Neutral
    }
}

@Composable
private fun AmbientCreaturesLayer(
    cameraX: Float,
    cameraY: Float,
    worldWidthPx: Float,
    worldHeightPx: Float,
    playerPosition: Offset,
    playerSizeDp: Float,
    playerSizePx: Float,
    consumedCreatureNames: Set<String>,
    interactionTick: Int,
    modifier: Modifier = Modifier,
    onCreatureClick: (FishInfo) -> Unit,
    onPreyEaten: (AmbientCreature) -> Unit,
    onPredatorHit: (AmbientCreature) -> Unit
) {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "ambient creatures")
    val swimClock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient swim clock"
    )

    Box(
        modifier = modifier.onSizeChanged { layerSize = it }
    ) {
        val viewportWidth = layerSize.width.toFloat()
        val viewportHeight = layerSize.height.toFloat()
        if (viewportWidth <= 0f || viewportHeight <= 0f) return@Box

        val playerScreenPosition = Offset(playerPosition.x - cameraX, playerPosition.y - cameraY)
        var preyContact: AmbientCreature? = null
        var predatorContact: AmbientCreature? = null

        AmbientCreatures.forEachIndexed { index, creature ->
            if (creature.name in consumedCreatureNames) return@forEachIndexed

            val displaySizeDp = ambientDisplaySizeDp(creature)
            val creatureSizePx = with(density) { displaySizeDp.dp.toPx() }
            val role = ambientCreatureRole(creature, playerSizeDp)
            val worldY = worldYForDepth(creature.depthMeters, worldHeightPx)
            val wave = sin(swimClock * 6.28f * (1.2f + creature.speed) + index) *
                creature.verticalDriftPx
            val screenY = worldY - cameraY + wave

            if (screenY in -creatureSizePx..(viewportHeight + creatureSizePx)) {
                val cycleSpeed = if (role == AmbientCreatureRole.Predator) {
                    0.78f + creature.speed * 2.55f
                } else {
                    0.58f + creature.speed * 1.9f
                }
                val cycle = (swimClock * cycleSpeed + creature.phase) % 1f
                val travel = worldWidthPx + creatureSizePx * 2f
                val leftToRightX = -creatureSizePx + cycle * travel
                val worldX = if (creature.swimsRight) {
                    leftToRightX
                } else {
                    worldWidthPx + creatureSizePx - cycle * travel
                }
                val screenX = worldX - cameraX
                var adjustedX = screenX
                var adjustedY = screenY
                val creatureCenter = Offset(
                    x = screenX + creatureSizePx / 2f,
                    y = screenY
                )
                val toPlayer = playerScreenPosition - creatureCenter
                val distanceToPlayer = toPlayer.getDistance().coerceAtLeast(1f)

                if (role == AmbientCreatureRole.Predator) {
                    val chaseRange = (viewportWidth * 0.95f + viewportHeight * 0.45f)
                        .coerceAtLeast(520f)
                    val chasePower = (1f - distanceToPlayer / chaseRange).coerceIn(0f, 1f)
                    if (chasePower > 0f) {
                        val horizontalPull = 0.2f + chasePower * 0.68f + creature.speed * 0.08f
                        val verticalPull = 0.16f + chasePower * 0.5f
                        val lunge = sin(swimClock * 12.56f + index) * 18f * chasePower
                        adjustedX += toPlayer.x * horizontalPull + if (toPlayer.x >= 0f) lunge else -lunge
                        adjustedY += toPlayer.y * verticalPull
                    }
                }

                if (role == AmbientCreatureRole.Prey && distanceToPlayer < 260f) {
                    val push = (1f - distanceToPlayer / 260f).coerceIn(0f, 1f)
                    adjustedX -= toPlayer.x / distanceToPlayer * 86f * push
                    adjustedY -= toPlayer.y / distanceToPlayer * 46f * push
                }

                val adjustedCenter = Offset(
                    x = adjustedX + creatureSizePx / 2f,
                    y = adjustedY
                )
                val contactDistance = (playerScreenPosition - adjustedCenter).getDistance()
                if (role == AmbientCreatureRole.Prey &&
                    contactDistance < (playerSizePx + creatureSizePx) * 0.31f
                ) {
                    preyContact = creature
                }
                if (role == AmbientCreatureRole.Predator &&
                    contactDistance < (playerSizePx + creatureSizePx) * 0.34f
                ) {
                    predatorContact = creature
                }

                val depthFade = (1f - creature.depthMeters / MaxDiveDepthMeters * 0.34f)
                    .coerceIn(0.58f, 1f)
                val facesRight = when {
                    role == AmbientCreatureRole.Predator -> playerScreenPosition.x > adjustedCenter.x
                    role == AmbientCreatureRole.Prey && distanceToPlayer < 260f ->
                        playerScreenPosition.x < adjustedCenter.x

                    else -> creature.swimsRight
                }

                if (adjustedX in -creatureSizePx..(viewportWidth + creatureSizePx)) {
                    Box(
                        modifier = Modifier
                            .size(displaySizeDp.dp)
                            .offset {
                                IntOffset(
                                    x = adjustedX.roundToInt(),
                                    y = (adjustedY - creatureSizePx / 2f).roundToInt()
                                )
                            }
                            .clickable {
                                onCreatureClick(creature.toFishInfo())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(creature.imageRes),
                            contentDescription = creature.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = depthFade
                                    scaleX = if (facesRight) -1f else 1f
                                    rotationZ = sin(swimClock * 6.28f + index) *
                                        if (role == AmbientCreatureRole.Predator) 2.2f else 3.5f
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        LaunchedEffect(preyContact?.name) {
            preyContact?.let(onPreyEaten)
        }
        LaunchedEffect(predatorContact?.name, interactionTick) {
            predatorContact?.let(onPredatorHit)
        }
    }
}

@Composable
private fun VitalStatusHud(
    health: Float,
    comfort: Float,
    hunger: Int,
    energy: Int,
    score: Int,
    comfortRange: ComfortRange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "PUAN $score",
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
        CornerStatusBar(
            label = "SAĞLIK",
            value = health,
            color = Color(0xFFE63946)
        )
        CornerStatusBar(
            label = "RAHATLIK",
            value = comfort,
            color = Color(0xFF06D6A0),
            footer = "Güvenli: ${comfortRange.label}"
        )
        CornerStatusBar(
            label = "ENERJİ",
            value = energy.toFloat(),
            color = Color(0xFF4CC9F0)
        )
        CornerStatusBar(
            label = "AÇLIK",
            value = hunger.toFloat(),
            color = Color(0xFFFFB703)
        )
    }
}

@Composable
private fun CornerStatusBar(
    label: String,
    value: Float,
    color: Color,
    footer: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(color)
            )
        }
        footer?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DepthGauge(
    depthMeters: Float,
    depthZone: DepthZone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null
    ) {
        Column(
            modifier = Modifier
                .width(74.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${depthMeters.roundToInt()} m",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Canvas(
                modifier = Modifier
                    .width(46.dp)
                    .height(210.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val progress = (depthMeters / MaxDiveDepthMeters).coerceIn(0f, 1f)
                val barWidth = 5.dp.toPx()
                val centerX = size.width / 2f

                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF64D2FF),
                            Color(0xFF1B4965),
                            Color(0xFF060A16)
                        )
                    ),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )

                repeat(5) { index ->
                    val y = size.height * (index / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.32f),
                        start = Offset(centerX - 8.dp.toPx(), y),
                        end = Offset(centerX + 8.dp.toPx(), y),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val markerY = size.height * progress
                drawCircle(
                    color = depthZone.color.copy(alpha = 0.28f),
                    radius = 13.dp.toPx(),
                    center = Offset(centerX, markerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, markerY)
                )
            }
        }
    }
}

@Composable
private fun EventLogBar(
    messages: List<EventLogEntry>,
    isLoading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(messages.size, isLoading) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.28f)
            .widthIn(max = 380.dp)
            .height(184.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xB7051720)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0x9D092433))
                    .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    messages.forEachIndexed { index, message ->
                        EventLogLine(
                            entry = message,
                            isLatest = index == messages.lastIndex,
                            accent = accent
                        )
                    }
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = accent,
                                strokeWidth = 2.dp
                            )
                            EventLogLine(
                                entry = EventLogEntry(
                                    type = EventLogType.Info,
                                    message = "Olay hazırlanıyor"
                                ),
                                isLatest = true,
                                accent = accent
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    val thumbFraction = if (scrollState.maxValue > 0) 0.34f else 1f
                    val scrollProgress = if (scrollState.maxValue > 0) {
                        scrollState.value / scrollState.maxValue.toFloat()
                    } else {
                        0f
                    }
                    val travelPx = with(density) { maxHeight.toPx() } * (1f - thumbFraction)
                    val thumbOffsetPx = (travelPx * scrollProgress).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = thumbOffsetPx) }
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(thumbFraction)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (scrollState.maxValue > 0) 0.9f else 0.42f))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xC70B2631))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF9BD4E8),
                                fontWeight = FontWeight.Black
                            )
                        ) {
                            append("Sea: ")
                        }
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.72f))) {
                            append("olay akışı")
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

@Composable
private fun EventLogLine(
    entry: EventLogEntry,
    isLatest: Boolean,
    accent: Color
) {
    val labelColor = eventLogTypeColor(entry.type, accent)

    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = labelColor.copy(alpha = if (isLatest) 1f else 0.78f),
                    fontWeight = FontWeight.Black
                )
            ) {
                append(entry.type.label)
            }
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f))) {
                append(": ")
                append(entry.message)
            }
        },
        color = Color.White,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium
    )
}

private fun eventLogTypeColor(type: EventLogType, accent: Color): Color = when (type) {
    EventLogType.Info -> Color(0xFF9BD4E8)
    EventLogType.Warning -> Color(0xFFFFC857)
    EventLogType.Ideal -> Color(0xFF06D6A0)
    EventLogType.Extra -> accent
}

@Composable
private fun FishEventCard(
    event: Event,
    depthZone: DepthZone,
    accent: Color,
    onOptionSelected: (Option) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth(0.74f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0061927)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bilgi Görevi",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = depthZone.name,
                        color = depthZone.color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = event.text,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                event.options.forEach { option ->
                    Button(
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.13f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = option.text,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverSummaryCard(
    fish: PlayableFish,
    state: GameState,
    choices: List<ChoiceRecord>,
    onChangeFish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val overlayInteractionSource = remember { MutableInteractionSource() }
    val learningScore = learningScoreFor(choices)
    val correctCount = choices.count { it.assessment == ChoiceAssessment.Correct }
    val riskyOrWrongCount = choices.count {
        it.assessment == ChoiceAssessment.Risky || it.assessment == ChoiceAssessment.Wrong
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = overlayInteractionSource,
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.9f)
                .heightIn(max = 590.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF4061927)),
            border = BorderStroke(1.dp, fish.accent.copy(alpha = 0.62f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Dalış Özeti",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${fish.name} ile sağlık sıfırlandı. Şimdi seçimlerin ne kadar doğruydu bakalım.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetric(
                        label = "Puan",
                        value = state.score.toString(),
                        accent = fish.accent,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetric(
                        label = "Derinlik",
                        value = "${state.depth} m",
                        accent = fish.accent,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetric(
                        label = "Bilgi",
                        value = "$learningScore/100",
                        accent = fish.accent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "$correctCount doğru seçim, $riskyOrWrongCount riskli veya yanlış seçim.",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (choices.isEmpty()) {
                        Text(
                            text = "Bu turda soru yanıtlamadan sağlık sıfırlandı. Bir sonraki denemede balığın güvenli derinliğini, enerjisini ve avcıları daha yakından takip et.",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        choices.forEachIndexed { index, choice ->
                            ChoiceSummaryRow(
                                index = index + 1,
                                choice = choice,
                                accent = fish.accent
                            )
                        }
                    }
                }

                Button(
                    onClick = onChangeFish,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = fish.accent.copy(alpha = 0.9f),
                        contentColor = Color(0xFF03131B)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Yeni deneme",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChoiceSummaryRow(
    index: Int,
    choice: ChoiceRecord,
    accent: Color
) {
    val statusColor = assessmentColor(choice.assessment, accent)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.075f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$index. ${choice.assessment.label}",
                color = statusColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${choice.depthMeters} m",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = choice.eventText,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Seçimin: ${choice.optionText}",
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = choice.feedback,
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Etki: Puan ${formatSignedDelta(choice.scoreDelta)} | Sağlık ${formatSignedDelta(choice.healthDelta)} | Rahatlık ${formatSignedDelta(choice.comfortDelta)} | Açlık ${formatSignedDelta(choice.hungerDelta)} | Enerji ${formatSignedDelta(choice.energyDelta)}",
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun LearningInfoCardView(
    info: LearningInfoCard,
    accent: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelColor = eventLogTypeColor(info.type, accent)

    Card(
        modifier = modifier
            .widthIn(max = 430.dp)
            .fillMaxWidth(0.86f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0061927)),
        border = BorderStroke(1.dp, labelColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = info.type.label,
                    color = labelColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = info.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = info.message,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = labelColor.copy(alpha = 0.9f),
                    contentColor = Color(0xFF03131B)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(
                    text = "Anladım",
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun FishInfoCard(
    info: FishInfo,
    onDismiss: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(430.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEA061927)),
        border = BorderStroke(1.dp, info.accent.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.habitat,
                        color = info.accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onChatClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = info.accent.copy(alpha = 0.76f),
                            contentColor = Color(0xFF04111B)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Sohbet Et", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Kapat")
                    }
                }
            }

            FishInfoRow(label = "Derinlik", value = info.depthRange)
            FishInfoRow(label = "Beslenme Tipi", value = info.dietType)
            FishInfoRow(label = "Ne Yer?", value = info.food)
            FishInfoRow(label = "Rol", value = info.ecologicalRole)

            Text(
                text = info.note,
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FishChatPanel(
    info: FishInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var messageText by remember(info.name) { mutableStateOf("") }
    var isSending by remember(info.name) { mutableStateOf(false) }
    var messages by remember(info.name) {
        mutableStateOf(
            listOf(
                FishChatMessage(
                    role = FishChatRole.Fish,
                    text = "Merhaba, ben ${info.name}. Bana yaşadığım derinliği, ne yediğimi ya da okyanusta nasıl hayatta kaldığımı sorabilirsin."
                )
            )
        )
    }

    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun sendMessage() {
        val trimmed = messageText.trim()
        if (trimmed.isBlank() || isSending) return

        val historyBeforeUserMessage = messages
        messages = historyBeforeUserMessage + FishChatMessage(FishChatRole.User, trimmed)
        messageText = ""
        isSending = true

        scope.launch {
            val answer = OpenAiFishChatClient
                .sendMessage(
                    personaPrompt = info.toPersonaPrompt(),
                    history = historyBeforeUserMessage,
                    userMessage = trimmed
                )
                .getOrElse { throwable ->
                    "Şu an su altı bağlantım zayıf: ${throwable.message ?: "biraz sonra tekrar dener misin?"}"
                }

            messages = messages + FishChatMessage(FishChatRole.Fish, answer)
            isSending = false
        }
    }

    Card(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(0.78f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0061927)),
        border = BorderStroke(1.dp, info.accent.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${info.name} ile Sohbet",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${info.depthRange} / ${info.habitat}",
                        color = info.accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "Kapat")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 158.dp, max = 238.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messages.forEach { message ->
                    FishChatBubble(
                        message = message,
                        accent = info.accent
                    )
                }
                if (isSending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = info.accent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    enabled = !isSending,
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    placeholder = {
                        Text(
                            text = "Balığa bir şey sor...",
                            color = Color.White.copy(alpha = 0.54f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = info.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        cursorColor = info.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = ::sendMessage,
                    enabled = messageText.isNotBlank() && !isSending,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = info.accent,
                        contentColor = Color(0xFF04111B),
                        disabledContainerColor = Color.White.copy(alpha = 0.12f),
                        disabledContentColor = Color.White.copy(alpha = 0.42f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    Text(text = "Gönder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FishChatBubble(
    message: FishChatMessage,
    accent: Color
) {
    val isUser = message.role == FishChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isUser) {
                        Color.White.copy(alpha = 0.16f)
                    } else {
                        accent.copy(alpha = 0.18f)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatScoreDelta(delta: Int): String =
    if (delta >= 0) "+$delta" else delta.toString()

private fun formatSignedDelta(delta: Int): String =
    if (delta > 0) "+$delta" else delta.toString()

private fun preyRespawnDelaySeconds(creatureName: String): Int =
    30 + (creatureName.fold(0) { total, char -> total + char.code } % 11)

private fun learningScoreFor(choices: List<ChoiceRecord>): Int {
    if (choices.isEmpty()) return 0
    val points = choices.sumOf { choice ->
        when (choice.assessment) {
            ChoiceAssessment.Correct -> 1.0
            ChoiceAssessment.Partial -> 0.65
            ChoiceAssessment.Risky -> 0.3
            ChoiceAssessment.Wrong -> 0.0
        }
    }
    return ((points / choices.size) * 100).roundToInt()
}

private fun assessmentColor(
    assessment: ChoiceAssessment,
    accent: Color
): Color = when (assessment) {
    ChoiceAssessment.Correct -> Color(0xFF06D6A0)
    ChoiceAssessment.Partial -> accent
    ChoiceAssessment.Risky -> Color(0xFFFFC857)
    ChoiceAssessment.Wrong -> Color(0xFFE63946)
}

private fun startingScenarioFor(fish: PlayableFish): String {
    val diet = dietProfileFor(fish.name, fish.habitat)
    val seaEvent = when {
        fish.habitat.contains("Reef", ignoreCase = true) ->
            "Resif çevresinde akıntı yön değiştiriyor; küçük avlar mercan boşluklarına saklanıyor."

        fish.habitat.contains("Open", ignoreCase = true) ||
            fish.habitat.contains("Current", ignoreCase = true) ->
            "Açık denizde sürüler dağılmış, uzakta güçlü bir akıntı besin izlerini taşıyor."

        fish.habitat.contains("Deep", ignoreCase = true) ||
            fish.habitat.contains("Night", ignoreCase = true) ||
            fish.habitat.contains("Twilight", ignoreCase = true) ->
            "Işık azalıyor, parlayan küçük canlılar hareketleniyor ve enerji tasarrufu kritik hale geliyor."

        fish.habitat.contains("Kelp", ignoreCase = true) ||
            fish.habitat.contains("Seagrass", ignoreCase = true) ->
            "Yosunların arasında saklanma alanları var, ama dipteki hareketler yeni bir tehlike işareti olabilir."

        fish.habitat.contains("Cave", ignoreCase = true) ||
            fish.habitat.contains("Rock", ignoreCase = true) ->
            "Kayalık yarıklarda güvenli geçitler var, fakat görüş azalınca doğru rota seçmek önem kazanıyor."

        else ->
            "Denizde akıntılar değişiyor, av ve sığınak dengesi hızla önem kazanıyor."
    }

    return "${fish.habitat} bölgesindesin. $seaEvent ${diet.first} olarak ${diet.second} arayacaksın; güvenli derinlik ${comfortRangeFor(fish).label}."
}

private fun FishInfo.toPersonaPrompt(): String = """
    Sen SeaVerse oyununda "$name" adlı deniz canlısısın.
    Kullanıcıyla Türkçe konuş ve birinci tekil şahıs kullan.
    Cevapların kısa, sıcak, merak uyandırıcı ve çocuklara uygun olsun.
    Bilimsel doğruluktan kopmadan 2-4 cümleyle yanıt ver.

    Habitat: $habitat
    Derinlik: $depthRange
    Beslenme tipi: $dietType
    Ne yer: $food
    Ekolojik rol: $ecologicalRole
    Not: $note

    Bilmediğin bir konuda kesin konuşma; okyanusla bağlantılı eğlenceli bir ipucu ver.
""".trimIndent()

@Composable
private fun FishInfoRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PlayerHud(
    fish: PlayableFish,
    onChangeFish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xB8061927)),
            border = BorderStroke(1.dp, fish.accent.copy(alpha = 0.38f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(fish.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer(scaleX = -1f),
                    contentScale = ContentScale.Fit
                )
                Column {
                    Text(
                        text = fish.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fish.habitat,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Button(
            onClick = onChangeFish,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = "Change")
        }
    }
}

@Composable
private fun MovementJoystick(
    modifier: Modifier = Modifier,
    accent: Color,
    onVectorChange: (Offset) -> Unit
) {
    var controlSize by remember { mutableStateOf(IntSize.Zero) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val transition = rememberInfiniteTransition(label = "joystick pulse")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "joystick pulse value"
    ).value

    fun updateKnob(position: Offset) {
        val radius = min(controlSize.width, controlSize.height) * 0.34f
        val center = Offset(controlSize.width / 2f, controlSize.height / 2f)
        val raw = position - center
        val distance = raw.getDistance()
        val clamped = if (radius > 0f && distance > radius) {
            val scale = radius / distance
            Offset(raw.x * scale, raw.y * scale)
        } else {
            raw
        }

        knobOffset = clamped
        onVectorChange(
            if (radius > 0f) {
                Offset(
                    x = clamped.x / radius,
                    y = clamped.y / radius
                )
            } else {
                Offset.Zero
            }
        )
    }

    Canvas(
        modifier = modifier
            .size(138.dp)
            .onSizeChanged { controlSize = it }
            .pointerInput(controlSize) {
                detectDragGestures(
                    onDragStart = { updateKnob(it) },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onVectorChange(Offset.Zero)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onVectorChange(Offset.Zero)
                    },
                    onDrag = { change, _ ->
                        updateKnob(change.position)
                        change.consume()
                    }
                )
            }
    ) {
        val center = this.center
        val baseRadius = size.minDimension * 0.46f
        val knobRadius = size.minDimension * 0.18f

        drawCircle(
            color = Color(0xAA02111D),
            radius = baseRadius,
            center = center
        )
        drawCircle(
            color = accent.copy(alpha = 0.18f + pulse * 0.08f),
            radius = baseRadius * (0.82f + pulse * 0.08f),
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = baseRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(center.x - baseRadius * 0.58f, center.y),
            end = Offset(center.x + baseRadius * 0.58f, center.y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(center.x, center.y - baseRadius * 0.58f),
            end = Offset(center.x, center.y + baseRadius * 0.58f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.82f), accent.copy(alpha = 0.9f)),
                center = center + knobOffset,
                radius = knobRadius * 1.6f
            ),
            radius = knobRadius,
            center = center + knobOffset
        )
    }
}

@Composable
private fun OceanBackdrop(dimAmount: Float) {
    Image(
        painter = painterResource(R.drawable.sea_background),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = dimAmount.coerceIn(0f, 1f)))
    )
}

@Composable
private fun UnderwaterOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ocean ambience")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble drift"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light shimmer"
    )

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.04f + shimmer * 0.03f),
                    Color.Transparent,
                    Color(0xFF001827).copy(alpha = 0.18f)
                )
            )
        )

        repeat(6) { index ->
            val startX = size.width * (0.28f + index * 0.08f)
            val endX = size.width * (0.12f + index * 0.15f + shimmer * 0.05f)
            drawLine(
                color = Color.White.copy(alpha = 0.035f + shimmer * 0.025f),
                start = Offset(startX, 0f),
                end = Offset(endX, size.height * 0.72f),
                strokeWidth = (28f + index * 6f),
                cap = StrokeCap.Round
            )
        }

        repeat(24) { index ->
            val lane = index / 24f
            val wave = sin((drift * 6.28f) + index) * 18f
            val x = size.width * lane + wave + cos(index * 1.7f) * 34f
            val speed = 0.62f + (index % 5) * 0.12f
            val travel = size.height + 90f
            val y = size.height - ((drift * travel * speed + index * 97f) % travel)
            val radius = 2.2f + (index % 4) * 1.6f
            drawCircle(
                color = Color.White.copy(alpha = 0.13f),
                radius = radius,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = radius * 0.42f,
                center = Offset(x - radius * 0.24f, y - radius * 0.24f)
            )
        }

        drawRect(
            color = Color(0xFF02111D).copy(alpha = 0.13f),
            topLeft = Offset(0f, size.height * 0.86f),
            size = Size(size.width, size.height * 0.14f)
        )
    }
}

private fun depthZoneFor(depthMeters: Float): DepthZone =
    DepthZones.last { depthMeters >= it.startMeters }

private fun worldYForDepth(depthMeters: Float, worldHeightPx: Float): Float =
    (depthMeters / MaxDiveDepthMeters).coerceIn(0f, 1f) * worldHeightPx

private fun depthMetersForPosition(
    positionY: Float,
    fishSizePx: Float,
    worldHeightPx: Float
): Float =
    if (worldHeightPx > fishSizePx) {
        ((positionY - fishSizePx / 2f) / (worldHeightPx - fishSizePx))
            .coerceIn(0f, 1f) * MaxDiveDepthMeters
    } else {
        0f
    }

private fun comfortRangeFor(fish: PlayableFish): ComfortRange {
    val habitat = fish.habitat
    val maxDepth = when {
        habitat.contains("Seagrass", ignoreCase = true) -> 80f
        habitat.contains("Kelp", ignoreCase = true) -> 120f
        habitat.contains("Lagoon", ignoreCase = true) -> 140f
        habitat.contains("Reef", ignoreCase = true) -> 260f
        habitat.contains("Shelf", ignoreCase = true) -> 320f
        habitat.contains("Rock", ignoreCase = true) -> 520f
        habitat.contains("Cave", ignoreCase = true) -> 650f
        habitat.contains("Open", ignoreCase = true) -> 900f
        habitat.contains("Current", ignoreCase = true) -> 1100f
        habitat.contains("Cold", ignoreCase = true) -> 1250f
        habitat.contains("Twilight", ignoreCase = true) -> 1800f
        habitat.contains("Night", ignoreCase = true) -> 2200f
        habitat.contains("Deep", ignoreCase = true) -> 3000f
        else -> 500f
    }
    return ComfortRange(
        label = "0-${maxDepth.roundToInt()} m",
        maxComfortDepthMeters = maxDepth
    )
}

private fun PlayableFish.toFishInfo(currentDepthMeters: Float): FishInfo {
    val diet = dietProfileFor(name, habitat)
    return FishInfo(
        name = name,
        habitat = habitat,
        depthRange = "${depthRangeForHabitat(habitat)} - şu an ${currentDepthMeters.roundToInt()} m",
        dietType = diet.first,
        food = diet.second,
        ecologicalRole = roleForHabitat(habitat),
        note = "$personality. Bu türle oynarken hız, çeviklik ve dayanıklılık kararlarını dengede tutmalısın.",
        accent = accent
    )
}

private fun AmbientCreature.toFishInfo(): FishInfo {
    val zone = depthZoneFor(depthMeters)
    val diet = dietProfileFor(name, habitatForDepth(depthMeters))
    return FishInfo(
        name = name,
        habitat = habitatForDepth(depthMeters),
        depthRange = "~${depthMeters.roundToInt()} m (${zone.name})",
        dietType = diet.first,
        food = diet.second,
        ecologicalRole = roleForDepth(depthMeters),
        note = ambientNoteFor(depthMeters),
        accent = zone.color
    )
}

private fun depthRangeForHabitat(habitat: String): String = when {
    habitat.contains("Open", ignoreCase = true) -> "0-700 m"
    habitat.contains("Deep", ignoreCase = true) -> "700-3000 m"
    habitat.contains("Night", ignoreCase = true) -> "800-1800 m"
    habitat.contains("Twilight", ignoreCase = true) -> "200-1000 m"
    habitat.contains("Cold", ignoreCase = true) -> "200-1200 m"
    habitat.contains("Kelp", ignoreCase = true) -> "0-80 m"
    habitat.contains("Seagrass", ignoreCase = true) -> "0-50 m"
    habitat.contains("Cave", ignoreCase = true) -> "50-500 m"
    habitat.contains("Reef", ignoreCase = true) -> "0-200 m"
    habitat.contains("Lagoon", ignoreCase = true) -> "0-60 m"
    habitat.contains("Rock", ignoreCase = true) -> "20-300 m"
    else -> "0-400 m"
}

private fun habitatForDepth(depthMeters: Float): String = when {
    depthMeters < 200f -> "Mercan resifi ve sığ güneşli sular"
    depthMeters < 1000f -> "Twilight zone, açık su ve dik resif yamaçları"
    depthMeters < 3000f -> "Midnight zone, karanlık açık deniz"
    else -> "Abyssal zone, soğuk derin deniz tabanı"
}

private fun dietProfileFor(name: String, habitat: String): Pair<String, String> = when {
    name.contains("Shark", ignoreCase = true) ->
        "Etçil" to "Küçük balıklar, kalamar ve zayıf sürü bireyleri"

    name.contains("Needle", ignoreCase = true) || name.contains("Yellowfin", ignoreCase = true) ->
        "Hızlı avcı" to "Küçük balıklar, kril ve planktonik kabuklular"

    name.contains("Lantern", ignoreCase = true) || name.contains("Violet", ignoreCase = true) ->
        "Derin su etçili" to "Mikro kabuklular, larvalar ve küçük derin deniz canlıları"

    name.contains("Grouper", ignoreCase = true) || name.contains("Snapper", ignoreCase = true) ->
        "Etçil / dip avcısı" to "Yengeç, karides, küçük balık ve yumuşakçalar"

    name.contains("Tank", ignoreCase = true) || name.contains("Guardian", ignoreCase = true) ->
        "Omnivor" to "Alg, küçük kabuklular ve resifteki organik parçacıklar"

    name.contains("Glider", ignoreCase = true) || name.contains("Icefin", ignoreCase = true) ->
        "Plankton avcısı" to "Zooplankton, küçük karidesler ve su kolonundaki larvalar"

    habitat.contains("Reef", ignoreCase = true) || habitat.contains("resif", ignoreCase = true) ->
        "Omnivor" to "Alg, plankton, küçük kabuklular ve resif canlıları"

    else ->
        "Fırsatçı beslenme" to "Plankton, küçük balıklar ve bulunduğu derinlikteki küçük canlılar"
}

private fun roleForHabitat(habitat: String): String = when {
    habitat.contains("Reef", ignoreCase = true) || habitat.contains("Lagoon", ignoreCase = true) ->
        "Resifte enerji akışını taşır; hem avcı hem de av olabilir."

    habitat.contains("Open", ignoreCase = true) || habitat.contains("Current", ignoreCase = true) ->
        "Açık denizde sürü hareketi ve avcı-av dengesi için önemlidir."

    habitat.contains("Deep", ignoreCase = true) || habitat.contains("Night", ignoreCase = true) ->
        "Derin suda az ışıkta besin zincirini canlı tutar."

    else ->
        "Bulunduğu habitatta besin zincirinin orta halkalarından biridir."
}

private fun roleForDepth(depthMeters: Float): String = when {
    depthMeters < 200f -> "Sığ bölgede resif besin zincirinin hareketli halkasıdır."
    depthMeters < 1000f -> "Twilight zone'da yüzeyden derine enerji taşıyan göçmen türlerdendir."
    depthMeters < 3000f -> "Midnight zone'da az ışıklı ortamda avcı-av dengesini kurar."
    else -> "Abyssal bölgede kıt besinle yaşayan derin deniz ağının parçasıdır."
}

private fun ambientNoteFor(depthMeters: Float): String = when {
    depthMeters < 200f -> "Bu bölgede ışık bol olduğu için bitkiler, resifler ve küçük avlar daha yoğundur."
    depthMeters < 1000f -> "Bu derinlikte ışık hızla azalır; birçok canlı saklanmak ve avlanmak için dikey göç yapar."
    depthMeters < 3000f -> "Bu katmanda karanlık baskındır; canlılar enerji tasarrufu ve hassas duyularla hayatta kalır."
    else -> "Burada basınç yüksek, sıcaklık düşük ve besin azdır; yavaş hareket etmek büyük avantajdır."
}

private fun oceanColorAt(progress: Float): Color {
    val clamped = progress.coerceIn(0f, 1f)
    return when {
        clamped < 0.12f -> blendColor(
            start = Color(0xFF3ED8FF),
            end = Color(0xFF0878B7),
            fraction = clamped / 0.12f
        )

        clamped < 0.32f -> blendColor(
            start = Color(0xFF0878B7),
            end = Color(0xFF073B66),
            fraction = (clamped - 0.12f) / 0.2f
        )

        clamped < 0.64f -> blendColor(
            start = Color(0xFF073B66),
            end = Color(0xFF061A33),
            fraction = (clamped - 0.32f) / 0.32f
        )

        else -> blendColor(
            start = Color(0xFF061A33),
            end = Color(0xFF01040A),
            fraction = (clamped - 0.64f) / 0.36f
        )
    }
}

private fun blendColor(start: Color, end: Color, fraction: Float): Color {
    val amount = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * amount,
        green = start.green + (end.green - start.green) * amount,
        blue = start.blue + (end.blue - start.blue) * amount,
        alpha = start.alpha + (end.alpha - start.alpha) * amount
    )
}
