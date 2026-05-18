package com.serhaterdem.seaverse.ui.arpreview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.serhaterdem.seaverse.ui.game.PlayableFish
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.PI
import kotlin.math.cos

// SceneView 2.2.1 API — verified against decompiled .aar:
//   Scene() composable (not SceneView), nodes via childNodes: List<Node>,
//   modelLoader.createModel(path) → FilamentAsset → .getInstance() → FilamentInstance,
//   node.rotation is Float3(pitchDeg, yawDeg, 0f) in Euler degrees.

@Composable
internal fun FishModelViewer(
    fish: PlayableFish,
    modifier: Modifier = Modifier
) {
    if (fish.modelAsset != null) {
        ThreeDFishViewer(fish = fish, modifier = modifier)
    } else {
        TwoDFishFallback(fish = fish, modifier = modifier)
    }
}

@Composable
private fun ThreeDFishViewer(fish: PlayableFish, modifier: Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val nodes = rememberNodes()

    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose { nodes.clear() }
    }

    LaunchedEffect(fish.modelAsset) {
        nodes.clear()
        fish.modelAsset?.let { path ->
            runCatching {
                val asset = modelLoader.createModel(path)
                ModelNode(
                    modelInstance = asset.getInstance(),
                    scaleToUnits = 0.8f,
                    centerOrigin = Float3(0f, 0f, 0f)
                )
            }.onSuccess { node ->
                nodes.add(node)
            }
        }
    }

    Box(modifier = modifier) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = nodes,
            isOpaque = false,
            onFrame = { _ ->
                (nodes.firstOrNull() as? ModelNode)?.rotation = Float3(pitch, yaw, 0f)
            }
        )

        // Transparent drag-to-rotate overlay above the Scene
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        yaw += dragAmount.x * 0.4f
                        pitch = (pitch + dragAmount.y * 0.2f).coerceIn(-45f, 45f)
                    }
                }
        )
    }
}

@Composable
private fun TwoDFishFallback(fish: PlayableFish, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "spin2d")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(fish.imageRes),
            contentDescription = fish.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.5f)
                .graphicsLayer(scaleX = cos(angle))
        )
    }
}
