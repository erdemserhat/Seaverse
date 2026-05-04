package com.serhaterdem.seaverse.ui.game

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serhaterdem.seaverse.R
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sceneSize = it }
    ) {
        val density = LocalDensity.current
        val fishSize = if (maxWidth < 680.dp) 86.dp else 118.dp
        val fishSizePx = with(density) { fishSize.toPx() }

        LaunchedEffect(sceneSize, fish.id, fishSizePx) {
            if (sceneSize.width == 0 || sceneSize.height == 0) return@LaunchedEffect

            fun initialPosition() = Offset(
                x = sceneSize.width * 0.44f,
                y = sceneSize.height * 0.46f
            )

            if (playerPosition == null) {
                playerPosition = initialPosition()
            }

            var previousFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameNanos ->
                    if (previousFrameNanos != 0L) {
                        val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                        val input = currentJoystickVector
                        val deadZone = 0.05f

                        if (input.getDistance() > deadZone) {
                            val current = playerPosition ?: initialPosition()
                            val speed = fish.speedPxPerSecond * (0.78f + fish.agility * 0.22f)
                            val next = Offset(
                                x = current.x + input.x * speed * deltaSeconds,
                                y = current.y + input.y * speed * deltaSeconds
                            )
                            val halfFish = fishSizePx / 2f
                            playerPosition = Offset(
                                x = next.x.coerceIn(halfFish, sceneSize.width - halfFish),
                                y = next.y.coerceIn(halfFish, sceneSize.height - halfFish)
                            )
                            if (input.x > 0.08f) facingRight = true
                            if (input.x < -0.08f) facingRight = false
                        }
                    }
                    previousFrameNanos = frameNanos
                }
            }
        }

        OceanBackdrop(dimAmount = 0.08f)
        UnderwaterOverlay(modifier = Modifier.fillMaxSize())

        val position = playerPosition ?: Offset(
            x = sceneSize.width * 0.44f,
            y = sceneSize.height * 0.46f
        )

        Image(
            painter = painterResource(fish.imageRes),
            contentDescription = fish.name,
            modifier = Modifier
                .size(fishSize)
                .offset {
                    IntOffset(
                        x = (position.x - fishSizePx / 2f).roundToInt(),
                        y = (position.y - fishSizePx / 2f).roundToInt()
                    )
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

        MovementJoystick(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(24.dp),
            accent = fish.accent,
            onVectorChange = { joystickVector = it }
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
