package ai.seha.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Seha's assistant: a soft gradient sphere with two bright eyes. The colours drift slowly round
 * the sphere, it bobs a little, blinks now and then, and glances to one side while it waits.
 *
 * [mood] changes the eyes: "idle" looks about, "thinking" looks up and the colours spin faster,
 * "happy" squints into a smile.
 */
@Composable
fun Orb(modifier: Modifier = Modifier, mood: String = "idle", glow: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "orb")
    val spinSeconds = if (mood == "thinking") 4000 else 11000
    val spin by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(spinSeconds, easing = LinearEasing)),
        label = "spin",
    )
    val bob by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    // Blink: eyes stay open most of the time, close for a moment, sometimes twice.
    val blink by transition.animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 5200
                1f at 0
                1f at 3300
                0.08f at 3400
                1f at 3520
                1f at 3900
                0.08f at 4000
                1f at 4120
                1f at 5200
            },
        ),
        label = "blink",
    )
    // Glance: looks to one side, back to the middle, then the other side.
    val glance by transition.animateFloat(
        0f, 0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 9000
                0f at 0
                0f at 2500
                1f at 3100 using FastOutSlowInEasing
                1f at 4600
                0f at 5200 using FastOutSlowInEasing
                0f at 6500
                -1f at 7100 using FastOutSlowInEasing
                -1f at 8300
                0f at 9000 using FastOutSlowInEasing
            },
        ),
        label = "glance",
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val d = size.minDimension
            val r = d * 0.40f
            val centre = Offset(size.width / 2, size.height / 2 + bob * d * 0.012f)

            // the shadow the sphere casts on the page, tinted by the sphere
            if (glow) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x55B06BF0), Color(0x227A6BF5), Color.Transparent),
                        center = Offset(centre.x, centre.y + r * 0.85f), radius = r * 1.15f,
                    ),
                    radius = r * 1.15f, center = Offset(centre.x, centre.y + r * 0.85f),
                )
            }

            // the sphere: a base and three drifting colour clouds, clipped to the circle
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre - Offset(r, r), centre + Offset(r, r))) }
            clipPath(circle) {
                drawCircle(Color(0xFFC94FA3), r, centre)
                cloud(centre, r, spin, 0f, Color(0xFFFF6AA8), 0.70f)
                cloud(centre, r, spin, 120f, Color(0xFF6B6AF5), 0.85f)
                cloud(centre, r, spin, 240f, Color(0xFFFFA24A), 0.75f)
                cloud(centre, r, -spin * 0.6f, 60f, Color(0xFF8A3FF0), 0.55f, 0.35f)
                // light from the top left, shade at the bottom right
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)),
                        center = centre + Offset(-r * 0.45f, -r * 0.5f), radius = r * 0.9f,
                    ),
                    r, centre,
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x00000000), Color(0x33000000)),
                        center = centre + Offset(-r * 0.2f, -r * 0.2f), radius = r * 1.25f,
                    ),
                    r, centre,
                )
            }

            // the eyes: two soft petals, leaning towards each other
            val look = glance * r * 0.12f
            val eyeY = centre.y - r * 0.08f
            val gap = r * 0.36f
            val open = if (mood == "happy") 0.55f else blink
            eye(Offset(centre.x - gap + look, eyeY), r, open, mirrored = false, smile = mood == "happy", up = mood == "thinking")
            eye(Offset(centre.x + gap + look, eyeY), r, open, mirrored = true, smile = mood == "happy", up = mood == "thinking")
        }
    }
}

private fun DrawScope.cloud(centre: Offset, r: Float, spin: Float, phase: Float, color: Color, size: Float, orbit: Float = 0.55f) {
    val a = (spin + phase) * PI / 180.0
    val c = centre + Offset((cos(a) * r * orbit).toFloat(), (sin(a) * r * orbit).toFloat())
    drawCircle(
        Brush.radialGradient(listOf(color, color.copy(alpha = 0.55f), color.copy(alpha = 0f)), center = c, radius = r * size * 1.4f),
        radius = r * size * 1.4f, center = c,
    )
}

/** One eye: a rounded petal with a soft white glow, squashed vertically when blinking. */
private fun DrawScope.eye(at: Offset, r: Float, open: Float, mirrored: Boolean, smile: Boolean, up: Boolean) {
    val w = r * 0.22f
    val h = r * 0.34f
    val petal = Path().apply {
        moveTo(0f, -h)
        cubicTo(w * 0.9f, -h * 0.6f, w * 0.9f, h * 0.5f, 0f, h)
        cubicTo(-w * 0.9f, h * 0.5f, -w * 0.9f, -h * 0.6f, 0f, -h)
        close()
    }
    // happy eyes squint and lean in a little more
    val tilt = (if (mirrored) 14f else -14f) * if (smile) 1.6f else 1f
    translate(at.x, at.y + if (up) -r * 0.06f else 0f) {
        rotate(tilt, Offset.Zero) {
            scale(1f, open.coerceAtLeast(0.06f), Offset.Zero) {
                // glow
                drawCircle(Brush.radialGradient(listOf(Color(0x80FFFFFF), Color.Transparent), Offset.Zero, h * 1.5f), h * 1.5f, Offset.Zero)
                drawPath(petal, Color.White)
            }
        }
    }
}

/** A small orb for list rows and chat bubbles. */
@Composable
fun OrbAvatar(modifier: Modifier = Modifier, mood: String = "idle") {
    Orb(modifier = modifier, mood = mood, glow = false)
}
