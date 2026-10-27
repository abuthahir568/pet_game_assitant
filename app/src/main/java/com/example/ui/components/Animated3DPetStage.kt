package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.data.model.PetActionState
import com.example.data.model.PetSpecies
import com.example.data.model.parseHexColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Animated3DPetStage(
    species: PetSpecies,
    furColorHex: String,
    eyeColorHex: String,
    collarColorHex: String,
    equippedHatId: String,
    equippedOutfitId: String,
    actionState: PetActionState,
    mouthOpenAmount: Float,
    isListening: Boolean,
    onPetTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet_3d_rig")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val tailPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (actionState) {
                    PetActionState.LAUGHING, PetActionState.PLAYING, PetActionState.DANCING -> 280
                    PetActionState.SLEEPING -> 1600
                    else -> 650
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail"
    )

    val spinOrbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Natural eye blinking state
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(actionState) {
        while (true) {
            delay(3200L)
            isBlinking = true
            delay(140L)
            isBlinking = false
        }
    }

    // Touch heart burst animation
    var tapHeartTick by remember { mutableLongStateOf(0L) }
    var tapHeartProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tapHeartTick) {
        if (tapHeartTick > 0L) {
            for (step in 1..20) {
                tapHeartProgress = step / 20f
                delay(32L)
            }
            tapHeartProgress = 0f
        }
    }

    val baseFur = parseHexColor(furColorHex, parseHexColor(species.defaultFurHex))
    val highlightFur = Color(
        red = (baseFur.red + 0.25f).coerceAtMost(1f),
        green = (baseFur.green + 0.25f).coerceAtMost(1f),
        blue = (baseFur.blue + 0.25f).coerceAtMost(1f),
        alpha = 1f
    )
    val shadowFur = Color(
        red = (baseFur.red * 0.68f).coerceIn(0f, 1f),
        green = (baseFur.green * 0.68f).coerceIn(0f, 1f),
        blue = (baseFur.blue * 0.68f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val eyeColor = parseHexColor(eyeColorHex, parseHexColor(species.defaultEyeHex))
    val collarColor = parseHexColor(collarColorHex, Color(0xFFF43F5E))

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("pet_3d_canvas_stage")
            .semantics { contentDescription = "3D Animated ${species.displayName} virtual pet" }
            .pointerInput(species) {
                detectTapGestures {
                    tapHeartTick = System.currentTimeMillis()
                    onPetTapped()
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val cx = w * 0.5f

        val jumpOffset = when (actionState) {
            PetActionState.PLAYING, PetActionState.DANCING -> -unit * 0.08f * kotlin.math.abs(tailPhase)
            PetActionState.LAUGHING -> -unit * 0.045f * kotlin.math.abs(tailPhase)
            PetActionState.SLEEPING -> unit * 0.03f
            else -> -unit * 0.018f * breathPhase
        }

        val cy = h * 0.54f + jumpOffset
        val bodyTiltDeg = when (actionState) {
            PetActionState.DANCING -> tailPhase * 12f
            PetActionState.LAUGHING -> tailPhase * 5f
            else -> if (isListening) 6f else tailPhase * 1.8f
        }

        // 1. Dynamic 3D Floor Pedestal & Ambient Contact Shadow
        val shadowScale = 1f - (kotlin.math.abs(jumpOffset) / (unit * 0.4f)).coerceIn(0f, 0.35f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x440F172A), Color.Transparent),
                center = Offset(cx, h * 0.83f),
                radius = unit * 0.32f * shadowScale
            ),
            topLeft = Offset(cx - unit * 0.30f * shadowScale, h * 0.78f),
            size = Size(unit * 0.60f * shadowScale, unit * 0.11f * shadowScale)
        )

        // Listening sonar halo when microphone is active
        if (isListening) {
            val pulseRadius = unit * (0.36f + 0.08f * breathPhase)
            drawCircle(
                color = Color(0x336366F1),
                radius = pulseRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0x664F46E5),
                radius = pulseRadius * 0.86f,
                center = Offset(cx, cy),
                style = Stroke(width = unit * 0.012f)
            )
        }

        rotate(degrees = bodyTiltDeg, pivot = Offset(cx, cy + unit * 0.12f)) {
            // 2. Animated 3D Tail behind body
            drawPetTail(
                species = species,
                cx = cx,
                cy = cy,
                unit = unit,
                tailPhase = tailPhase,
                highlightFur = highlightFur,
                baseFur = baseFur,
                shadowFur = shadowFur
            )

            // 3. Optional Superhero Cape / Outfit Back Layer
            if (equippedOutfitId.contains("cape")) {
                val capePath = Path().apply {
                    moveTo(cx - unit * 0.16f, cy + unit * 0.02f)
                    lineTo(cx + unit * 0.16f, cy + unit * 0.02f)
                    lineTo(cx + unit * 0.25f + tailPhase * unit * 0.03f, cy + unit * 0.28f)
                    lineTo(cx - unit * 0.25f + tailPhase * unit * 0.03f, cy + unit * 0.28f)
                    close()
                }
                drawPath(
                    path = capePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF4338CA))
                    )
                )
            }

            // 4. 3D Shaded Torso / Belly
            val torsoWidth = unit * (0.35f + 0.012f * breathPhase)
            val torsoHeight = unit * (0.31f + 0.015f * breathPhase)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(highlightFur, baseFur, shadowFur),
                    center = Offset(cx - torsoWidth * 0.2f, cy + unit * 0.08f),
                    radius = torsoWidth * 0.9f
                ),
                topLeft = Offset(cx - torsoWidth * 0.5f, cy + unit * 0.01f),
                size = Size(torsoWidth, torsoHeight),
                cornerRadius = CornerRadius(torsoWidth * 0.46f, torsoWidth * 0.46f)
            )

            // Soft Cream Belly Patch (or Equipped Hoodie/Vest)
            val bellyColor = when {
                equippedOutfitId.contains("hoodie") -> Color(0xFFF472B6)
                equippedOutfitId.contains("tuxedo") -> Color(0xFF1E293B)
                species == PetSpecies.PENGUIN || species == PetSpecies.PANDA -> Color(0xFFF8FAFC)
                else -> Color(0xFFFFFBEB).copy(alpha = 0.78f)
            }
            drawOval(
                color = bellyColor,
                topLeft = Offset(cx - torsoWidth * 0.30f, cy + unit * 0.06f),
                size = Size(torsoWidth * 0.60f, torsoHeight * 0.72f)
            )

            // 5. Collar & Golden Tag
            drawRoundRect(
                color = collarColor,
                topLeft = Offset(cx - torsoWidth * 0.36f, cy + unit * 0.01f),
                size = Size(torsoWidth * 0.72f, unit * 0.042f),
                cornerRadius = CornerRadius(unit * 0.02f, unit * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFD97706)),
                    center = Offset(cx, cy + unit * 0.055f),
                    radius = unit * 0.028f
                ),
                radius = unit * 0.024f,
                center = Offset(cx, cy + unit * 0.055f)
            )

            // 6. 3D Feet & Animated Paws
            val footY = cy + unit * 0.27f
            drawOval(
                brush = Brush.radialGradient(listOf(highlightFur, shadowFur)),
                topLeft = Offset(cx - unit * 0.16f, footY),
                size = Size(unit * 0.12f, unit * 0.075f)
            )
            drawOval(
                brush = Brush.radialGradient(listOf(highlightFur, shadowFur)),
                topLeft = Offset(cx + unit * 0.04f, footY),
                size = Size(unit * 0.12f, unit * 0.075f)
            )

            val armWave = when (actionState) {
                PetActionState.SPEAKING, PetActionState.LAUGHING, PetActionState.DANCING -> -unit * 0.05f * kotlin.math.abs(tailPhase)
                else -> 0f
            }
            drawOval(
                brush = Brush.radialGradient(listOf(highlightFur, baseFur)),
                topLeft = Offset(cx - torsoWidth * 0.56f, cy + unit * 0.08f + armWave),
                size = Size(unit * 0.095f, unit * 0.13f)
            )
            drawOval(
                brush = Brush.radialGradient(listOf(highlightFur, baseFur)),
                topLeft = Offset(cx + torsoWidth * 0.46f - unit * 0.095f + unit * 0.095f, cy + unit * 0.08f - armWave),
                size = Size(unit * 0.095f, unit * 0.13f)
            )

            // 7. Species-Specific 3D Ears / Mane / Horns (Behind Head)
            val headCenterY = cy - unit * 0.13f
            val headRadius = unit * 0.21f
            drawSpeciesHeadFeatures(
                species = species,
                cx = cx,
                headY = headCenterY,
                headR = headRadius,
                earTwitch = tailPhase * 4f,
                highlightFur = highlightFur,
                baseFur = baseFur,
                shadowFur = shadowFur
            )

            // 8. 3D Volumetric Head Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(highlightFur, baseFur, shadowFur),
                    center = Offset(cx - headRadius * 0.32f, headCenterY - headRadius * 0.32f),
                    radius = headRadius * 1.45f
                ),
                radius = headRadius,
                center = Offset(cx, headCenterY)
            )

            // Panda dark eye patches or Tiger stripes
            if (species == PetSpecies.PANDA) {
                drawOval(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cx - headRadius * 0.62f, headCenterY - headRadius * 0.26f),
                    size = Size(headRadius * 0.46f, headRadius * 0.42f)
                )
                drawOval(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cx + headRadius * 0.16f, headCenterY - headRadius * 0.26f),
                    size = Size(headRadius * 0.46f, headRadius * 0.42f)
                )
            } else if (species == PetSpecies.TIGER) {
                drawLine(
                    color = Color(0xFF451A03),
                    start = Offset(cx, headCenterY - headRadius * 0.88f),
                    end = Offset(cx, headCenterY - headRadius * 0.52f),
                    strokeWidth = headRadius * 0.08f,
                    cap = StrokeCap.Round
                )
            }

            // Rosy 3D Cheeks
            drawOval(
                color = Color(0x55FB7185),
                topLeft = Offset(cx - headRadius * 0.72f, headCenterY + headRadius * 0.06f),
                size = Size(headRadius * 0.30f, headRadius * 0.18f)
            )
            drawOval(
                color = Color(0x55FB7185),
                topLeft = Offset(cx + headRadius * 0.42f, headCenterY + headRadius * 0.06f),
                size = Size(headRadius * 0.30f, headRadius * 0.18f)
            )

            // 9. Expressive Eyes & Eyebrows
            val eyeOffsetX = headRadius * 0.38f
            val eyeY = headCenterY - headRadius * 0.06f
            val eyeR = headRadius * 0.19f

            if (actionState == PetActionState.SLEEPING || isBlinking) {
                // Closed happy/sleepy curved eyelids
                drawArc(
                    color = Color(0xFF1E1B4B),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx - eyeOffsetX - eyeR, eyeY - eyeR * 0.5f),
                    size = Size(eyeR * 2f, eyeR * 1.2f),
                    style = Stroke(width = headRadius * 0.065f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF1E1B4B),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx + eyeOffsetX - eyeR, eyeY - eyeR * 0.5f),
                    size = Size(eyeR * 2f, eyeR * 1.2f),
                    style = Stroke(width = headRadius * 0.065f, cap = StrokeCap.Round)
                )
            } else {
                listOf(cx - eyeOffsetX, cx + eyeOffsetX).forEach { ex ->
                    // Sclera
                    drawCircle(color = Color.White, radius = eyeR, center = Offset(ex, eyeY))
                    // Colored Iris
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(eyeColor, Color(0xFF0F172A)),
                            center = Offset(ex, eyeY),
                            radius = eyeR * 0.9f
                        ),
                        radius = eyeR * 0.76f,
                        center = Offset(ex, eyeY)
                    )
                    // Pupil
                    drawCircle(color = Color(0xFF090D16), radius = eyeR * 0.42f, center = Offset(ex, eyeY))
                    // Dual 3D Specular Catchlights
                    drawCircle(
                        color = Color.White,
                        radius = eyeR * 0.25f,
                        center = Offset(ex - eyeR * 0.26f, eyeY - eyeR * 0.28f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = eyeR * 0.11f,
                        center = Offset(ex + eyeR * 0.25f, eyeY + eyeR * 0.22f)
                    )
                }
            }

            // Expressive Eyebrows (raise while speaking or listening)
            val browLift = if (actionState == PetActionState.SPEAKING || isListening) -headRadius * 0.08f else 0f
            drawLine(
                color = shadowFur,
                start = Offset(cx - eyeOffsetX - eyeR * 0.7f, eyeY - eyeR * 1.35f + browLift),
                end = Offset(cx - eyeOffsetX + eyeR * 0.7f, eyeY - eyeR * 1.48f + browLift),
                strokeWidth = headRadius * 0.06f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = shadowFur,
                start = Offset(cx + eyeOffsetX - eyeR * 0.7f, eyeY - eyeR * 1.48f + browLift),
                end = Offset(cx + eyeOffsetX + eyeR * 0.7f, eyeY - eyeR * 1.35f + browLift),
                strokeWidth = headRadius * 0.06f,
                cap = StrokeCap.Round
            )

            // 10. 3D Snout / Beak / Trunk & Real-Time Lip-Sync Mouth
            val muzzleY = headCenterY + headRadius * 0.26f
            drawOval(
                color = Color(0xFFFFFBEB),
                topLeft = Offset(cx - headRadius * 0.34f, muzzleY - headRadius * 0.16f),
                size = Size(headRadius * 0.68f, headRadius * 0.42f)
            )

            if (species == PetSpecies.PENGUIN || species == PetSpecies.PARROT) {
                // Cute glossy 3D beak
                drawOval(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(cx - headRadius * 0.20f, muzzleY - headRadius * 0.10f),
                    size = Size(headRadius * 0.40f, headRadius * 0.24f)
                )
            } else if (species == PetSpecies.ELEPHANT) {
                // Cute curled baby trunk
                drawRoundRect(
                    color = baseFur,
                    topLeft = Offset(cx - headRadius * 0.13f, muzzleY - headRadius * 0.12f),
                    size = Size(headRadius * 0.26f, headRadius * 0.46f),
                    cornerRadius = CornerRadius(headRadius * 0.13f, headRadius * 0.13f)
                )
            } else {
                // Glossy 3D Nose
                drawOval(
                    color = Color(0xFF1E1B4B),
                    topLeft = Offset(cx - headRadius * 0.14f, muzzleY - headRadius * 0.12f),
                    size = Size(headRadius * 0.28f, headRadius * 0.18f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = headRadius * 0.04f,
                    center = Offset(cx - headRadius * 0.05f, muzzleY - headRadius * 0.06f)
                )
            }

            // Dynamic Lip-Sync Mouth & Action Mouth
            val activeOpen = when {
                mouthOpenAmount > 0.05f -> mouthOpenAmount
                actionState == PetActionState.EATING || actionState == PetActionState.LAUGHING -> kotlin.math.abs(tailPhase)
                else -> 0f
            }

            if (activeOpen > 0.08f) {
                val mouthW = headRadius * 0.38f
                val mouthH = headRadius * (0.14f + 0.28f * activeOpen)
                drawOval(
                    color = Color(0xFF881337),
                    topLeft = Offset(cx - mouthW * 0.5f, muzzleY + headRadius * 0.08f),
                    size = Size(mouthW, mouthH)
                )
                // Pink tongue inside open mouth
                drawOval(
                    color = Color(0xFFFB7185),
                    topLeft = Offset(cx - mouthW * 0.34f, muzzleY + headRadius * 0.08f + mouthH * 0.45f),
                    size = Size(mouthW * 0.68f, mouthH * 0.50f)
                )
            } else {
                // Warm friendly W-smile
                drawArc(
                    color = Color(0xFF1E1B4B),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx - headRadius * 0.22f, muzzleY + headRadius * 0.02f),
                    size = Size(headRadius * 0.22f, headRadius * 0.18f),
                    style = Stroke(width = headRadius * 0.048f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF1E1B4B),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx, muzzleY + headRadius * 0.02f),
                    size = Size(headRadius * 0.22f, headRadius * 0.18f),
                    style = Stroke(width = headRadius * 0.048f, cap = StrokeCap.Round)
                )
            }

            // 11. Equipped Hat / Visor / Crown Accessories
            drawEquippedAccessories(
                equippedHatId = equippedHatId,
                cx = cx,
                headCenterY = headCenterY,
                eyeY = eyeY,
                headR = headRadius
            )
        }

        // 12. Action Particle Overlays (Bathing bubbles, Sleeping Zzz, Eating treats, Tap Hearts)
        if (actionState == PetActionState.BATHING) {
            for (i in 0..5) {
                val angle = (spinOrbit + i * 60f) * (Math.PI / 180.0)
                val bx = cx + cos(angle).toFloat() * unit * 0.26f
                val by = cy - unit * 0.08f + sin(angle).toFloat() * unit * 0.18f
                drawCircle(color = Color(0x88BAE6FD), radius = unit * 0.045f, center = Offset(bx, by))
                drawCircle(color = Color.White, radius = unit * 0.045f, center = Offset(bx, by), style = Stroke(width = 2f))
            }
        }

        if (tapHeartProgress > 0f) {
            val alpha = (1f - tapHeartProgress).coerceIn(0f, 1f)
            for (i in -1..1) {
                val hx = cx + i * unit * 0.16f
                val hy = cy - unit * (0.28f + 0.18f * tapHeartProgress)
                drawCircle(
                    color = Color(0xFFF43F5E).copy(alpha = alpha),
                    radius = unit * 0.032f,
                    center = Offset(hx, hy)
                )
            }
        }
    }
}

private fun DrawScope.drawPetTail(
    species: PetSpecies,
    cx: Float,
    cy: Float,
    unit: Float,
    tailPhase: Float,
    highlightFur: Color,
    baseFur: Color,
    shadowFur: Color
) {
    val tailBaseX = cx + unit * 0.13f
    val tailBaseY = cy + unit * 0.20f
    rotate(degrees = tailPhase * 24f, pivot = Offset(tailBaseX, tailBaseY)) {
        when (species.tailStyle) {
            "PUFF" -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White, highlightFur)),
                    radius = unit * 0.075f,
                    center = Offset(tailBaseX + unit * 0.06f, tailBaseY - unit * 0.02f)
                )
            }
            "RAINBOW", "NINE_TAIL" -> {
                drawOval(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFF472B6), Color(0xFFA78BFA), Color(0xFF38BDF8))
                    ),
                    topLeft = Offset(tailBaseX, tailBaseY - unit * 0.16f),
                    size = Size(unit * 0.18f, unit * 0.12f)
                )
            }
            else -> {
                drawOval(
                    brush = Brush.linearGradient(listOf(highlightFur, baseFur, shadowFur)),
                    topLeft = Offset(tailBaseX, tailBaseY - unit * 0.15f),
                    size = Size(unit * 0.17f, unit * 0.085f)
                )
            }
        }
    }
}

private fun DrawScope.drawSpeciesHeadFeatures(
    species: PetSpecies,
    cx: Float,
    headY: Float,
    headR: Float,
    earTwitch: Float,
    highlightFur: Color,
    baseFur: Color,
    shadowFur: Color
) {
    when (species.earStyle) {
        "MANE" -> {
            // Majestic Lion golden mane
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0)
                val mx = cx + cos(rad).toFloat() * headR * 0.92f
                val my = headY + sin(rad).toFloat() * headR * 0.92f
                drawCircle(color = Color(0xFFB45309), radius = headR * 0.36f, center = Offset(mx, my))
            }
        }
        "LONG_BUNNY" -> {
            rotate(degrees = -10f + earTwitch, pivot = Offset(cx - headR * 0.45f, headY - headR * 0.6f)) {
                drawOval(
                    color = baseFur,
                    topLeft = Offset(cx - headR * 0.65f, headY - headR * 1.75f),
                    size = Size(headR * 0.42f, headR * 1.15f)
                )
                drawOval(
                    color = Color(0xFFF9A8D4),
                    topLeft = Offset(cx - headR * 0.55f, headY - headR * 1.55f),
                    size = Size(headR * 0.22f, headR * 0.80f)
                )
            }
            rotate(degrees = 10f - earTwitch, pivot = Offset(cx + headR * 0.45f, headY - headR * 0.6f)) {
                drawOval(
                    color = baseFur,
                    topLeft = Offset(cx + headR * 0.23f, headY - headR * 1.75f),
                    size = Size(headR * 0.42f, headR * 1.15f)
                )
                drawOval(
                    color = Color(0xFFF9A8D4),
                    topLeft = Offset(cx + headR * 0.33f, headY - headR * 1.55f),
                    size = Size(headR * 0.22f, headR * 0.80f)
                )
            }
        }
        "HORN_EARS" -> {
            // Unicorn spiral golden horn + ears
            val horn = Path().apply {
                moveTo(cx, headY - headR * 1.65f)
                lineTo(cx - headR * 0.18f, headY - headR * 0.82f)
                lineTo(cx + headR * 0.18f, headY - headR * 0.82f)
                close()
            }
            drawPath(horn, brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFF472B6))))
            drawCircle(color = baseFur, radius = headR * 0.32f, center = Offset(cx - headR * 0.68f, headY - headR * 0.72f))
            drawCircle(color = baseFur, radius = headR * 0.32f, center = Offset(cx + headR * 0.68f, headY - headR * 0.72f))
        }
        "HORNS_WINGS" -> {
            // Dragon horns & cute side wings
            drawCircle(color = Color(0xFFF59E0B), radius = headR * 0.25f, center = Offset(cx - headR * 0.58f, headY - headR * 0.95f))
            drawCircle(color = Color(0xFFF59E0B), radius = headR * 0.25f, center = Offset(cx + headR * 0.58f, headY - headR * 0.95f))
            drawOval(color = Color(0xFFA78BFA), topLeft = Offset(cx - headR * 1.45f, headY + headR * 0.75f), size = Size(headR * 0.65f, headR * 0.45f))
            drawOval(color = Color(0xFFA78BFA), topLeft = Offset(cx + headR * 0.80f, headY + headR * 0.75f), size = Size(headR * 0.65f, headR * 0.45f))
        }
        "BIG_FLOPPY" -> {
            // Elephant big ears
            drawOval(color = baseFur, topLeft = Offset(cx - headR * 1.50f, headY - headR * 0.55f), size = Size(headR * 0.78f, headR * 1.15f))
            drawOval(color = baseFur, topLeft = Offset(cx + headR * 0.72f, headY - headR * 0.55f), size = Size(headR * 0.78f, headR * 1.15f))
        }
        "FLOPPY" -> {
            // Puppy floppy ears
            rotate(degrees = -14f + earTwitch, pivot = Offset(cx - headR * 0.7f, headY - headR * 0.4f)) {
                drawOval(color = shadowFur, topLeft = Offset(cx - headR * 1.15f, headY - headR * 0.45f), size = Size(headR * 0.46f, headR * 0.98f))
            }
            rotate(degrees = 14f - earTwitch, pivot = Offset(cx + headR * 0.7f, headY - headR * 0.4f)) {
                drawOval(color = shadowFur, topLeft = Offset(cx + headR * 0.69f, headY - headR * 0.45f), size = Size(headR * 0.46f, headR * 0.98f))
            }
        }
        "ROUND_DARK" -> {
            drawCircle(color = Color(0xFF1E293B), radius = headR * 0.36f, center = Offset(cx - headR * 0.72f, headY - headR * 0.72f))
            drawCircle(color = Color(0xFF1E293B), radius = headR * 0.36f, center = Offset(cx + headR * 0.72f, headY - headR * 0.72f))
        }
        "NONE" -> Unit
        else -> {
            // Pointed / Round / Antler / Crest cute ears
            drawCircle(
                brush = Brush.radialGradient(listOf(highlightFur, shadowFur)),
                radius = headR * 0.38f,
                center = Offset(cx - headR * 0.70f, headY - headR * 0.70f)
            )
            drawCircle(
                color = Color(0xFFF9A8D4),
                radius = headR * 0.20f,
                center = Offset(cx - headR * 0.70f, headY - headR * 0.70f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(highlightFur, shadowFur)),
                radius = headR * 0.38f,
                center = Offset(cx + headR * 0.70f, headY - headR * 0.70f)
            )
            drawCircle(
                color = Color(0xFFF9A8D4),
                radius = headR * 0.20f,
                center = Offset(cx + headR * 0.70f, headY - headR * 0.70f)
            )
        }
    }
}

private fun DrawScope.drawEquippedAccessories(
    equippedHatId: String,
    cx: Float,
    headCenterY: Float,
    eyeY: Float,
    headR: Float
) {
    if (equippedHatId.isBlank()) return
    when {
        equippedHatId.contains("crown") -> {
            val crownPath = Path().apply {
                val base = headCenterY - headR * 0.85f
                moveTo(cx - headR * 0.42f, base)
                lineTo(cx - headR * 0.48f, base - headR * 0.42f)
                lineTo(cx - headR * 0.18f, base - headR * 0.18f)
                lineTo(cx, base - headR * 0.52f)
                lineTo(cx + headR * 0.18f, base - headR * 0.18f)
                lineTo(cx + headR * 0.48f, base - headR * 0.42f)
                lineTo(cx + headR * 0.42f, base)
                close()
            }
            drawPath(crownPath, brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFD97706))))
            drawCircle(color = Color(0xFFF43F5E), radius = headR * 0.07f, center = Offset(cx, headCenterY - headR * 1.02f))
        }
        equippedHatId.contains("wizard") || equippedHatId.contains("party") -> {
            val coneColor = if (equippedHatId.contains("wizard")) Color(0xFF7C3AED) else Color(0xFFEC4899)
            val cone = Path().apply {
                val base = headCenterY - headR * 0.82f
                moveTo(cx, base - headR * 0.75f)
                lineTo(cx - headR * 0.46f, base)
                lineTo(cx + headR * 0.46f, base)
                close()
            }
            drawPath(cone, color = coneColor)
            drawCircle(color = Color(0xFFFDE047), radius = headR * 0.10f, center = Offset(cx, headCenterY - headR * 1.58f))
        }
        equippedHatId.contains("shades") -> {
            drawRoundRect(
                color = Color(0xCC06B6D4),
                topLeft = Offset(cx - headR * 0.65f, eyeY - headR * 0.16f),
                size = Size(headR * 1.30f, headR * 0.32f),
                cornerRadius = CornerRadius(headR * 0.12f, headR * 0.12f)
            )
        }
    }
}
