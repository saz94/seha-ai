package ai.seha.app.ui

import ai.seha.app.data.tr
import ai.seha.app.domain.Dates
import ai.seha.app.domain.trim1
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

/** Water for today: how much, how much more, and the glasses logged. */
@Composable
fun WaterScreen(vm: AppViewModel, nav: Nav) {
    val data = vm.data
    val s = data.settings
    val today = Dates.today()
    val had = data.water[today] ?: 0
    val target = vm.waterTarget
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var custom by remember { mutableStateOf(false) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Screen(title = tr("Water", "الماء"), onBack = { nav.back() }) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaterDrop(progress = if (target > 0) had.toFloat() / target else 0f, modifier = Modifier.size(150.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text((had / 1000.0).trim1() + " " + tr("L", "ل"), style = MaterialTheme.typography.displaySmall.tabular())
                    Hint(tr("of ${(target / 1000.0).trim1()} L today", "من ${(target / 1000.0).trim1()} ل اليوم"))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            had >= target -> tr("Target reached. Sip as you like.", "بلغت هدفك. اشرب كما تحب.")
                            else -> tr("${((target - had) / 1000.0).trim1()} L to go, about ${((target - had + s.glassMl - 1) / s.glassMl)} glasses.", "بقي ${((target - had) / 1000.0).trim1()} ل، نحو ${((target - had + s.glassMl - 1) / s.glassMl)} أكواب.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Heading(tr("Add", "أضف"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (ml in listOf(s.glassMl, 330, 500)) {
                GlassButton(ml, Modifier.weight(1f)) { vm.addWater(ml) }
            }
            GlassButton(0, Modifier.weight(1f)) { custom = true }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hint(tr("Your glass is ${s.glassMl} ml.", "كوبك ${s.glassMl} مل."), Modifier.weight(1f))
            Stepper(
                "${s.glassMl}",
                { vm.saveSettings(s.copy(glassMl = (s.glassMl - 50).coerceAtLeast(100))) },
                { vm.saveSettings(s.copy(glassMl = (s.glassMl + 50).coerceAtMost(1000))) },
            )
        }

        Heading(tr("Why this target", "لماذا هذا الهدف"))
        Panel {
            Text(
                tr(
                    "35 ml for every kilogram you weigh (${data.latestWeight.trim1()} kg), adjusted for how active your days are, plus 400 ml for a hot climate and 10 ml for every minute of today's workout (up to 90).",
                    "35 مل لكل كيلوغرام من وزنك (${data.latestWeight.trim1()} كغ)، مع تعديل بحسب نشاطك اليومي، و400 مل للمناخ الحار، و10 مل لكل دقيقة من تمرين اليوم (حتى 90 دقيقة).",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Hot climate", "مناخ حار"), style = MaterialTheme.typography.titleSmall)
                    Hint(tr("Adds 400 ml a day.", "يضيف 400 مل يومياً."))
                }
                Switch(checked = s.hotClimate, onCheckedChange = { vm.saveSettings(s.copy(hotClimate = it)) })
            }
        }

        Heading(tr("Reminders", "التذكيرات"))
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Remind me to drink", "ذكّرني بالشرب"), style = MaterialTheme.typography.titleSmall)
                    Hint(tr("Only when you are behind for the time of day.", "فقط عندما تكون متأخراً عن وقت اليوم."))
                }
                Switch(checked = s.waterReminder, onCheckedChange = { on ->
                    vm.saveSettings(s.copy(waterReminder = on))
                    if (on && Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            if (s.waterReminder) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Every", "كل"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Stepper(
                        tr("${s.waterEveryHours} h", "${s.waterEveryHours} س"),
                        { vm.saveSettings(s.copy(waterEveryHours = (s.waterEveryHours - 1).coerceAtLeast(1))) },
                        { vm.saveSettings(s.copy(waterEveryHours = (s.waterEveryHours + 1).coerceAtMost(6))) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("From", "من"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Stepper(
                        "%02d:00".format(java.util.Locale.ENGLISH, s.waterStartHour),
                        { vm.saveSettings(s.copy(waterStartHour = (s.waterStartHour - 1).coerceAtLeast(4))) },
                        { vm.saveSettings(s.copy(waterStartHour = (s.waterStartHour + 1).coerceAtMost(s.waterEndHour - 2))) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Until", "حتى"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Stepper(
                        "%02d:00".format(java.util.Locale.ENGLISH, s.waterEndHour),
                        { vm.saveSettings(s.copy(waterEndHour = (s.waterEndHour - 1).coerceAtLeast(s.waterStartHour + 2))) },
                        { vm.saveSettings(s.copy(waterEndHour = (s.waterEndHour + 1).coerceAtMost(23))) },
                    )
                }
            }
        }

        // ---- the week
        val week = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()).toString() }
        Heading(tr("Last 7 days", "آخر 7 أيام"))
        Panel {
            DayBars(week.map { (data.water[it] ?: 0).toFloat() }, color = LocalSehaColors.current.water)
            Spacer(Modifier.height(6.dp))
            val days = week.count { (data.water[it] ?: 0) >= target }
            Hint(tr("Reached the target on $days of the last 7 days.", "بلغت الهدف في $days من آخر 7 أيام."))
        }

        // ---- today's glasses
        val log = data.waterLog.filter { it.date == today }.sortedByDescending { it.time }
        if (log.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Heading(tr("Today", "اليوم"), Modifier.weight(1f))
                TextButton(onClick = { vm.undoWater() }) { Text(tr("Undo last", "تراجع عن الأخير")) }
            }
            Panel {
                for (entry in log) {
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(LocalSehaColors.current.water))
                        Spacer(Modifier.width(10.dp))
                        Text(Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).format(timeFormat), style = MaterialTheme.typography.bodyMedium.tabular(), modifier = Modifier.weight(1f))
                        Text("${entry.ml} " + tr("ml", "مل"), style = MaterialTheme.typography.titleSmall.tabular())
                    }
                }
            }
        }
    }

    if (custom) {
        var text by remember { mutableStateOf("") }
        val ml = text.toIntOrNull()
        AlertDialog(
            onDismissRequest = { custom = false },
            title = { Text(tr("Add water", "أضف ماء")) },
            text = { NumberField(tr("Amount", "الكمية"), text, { text = it }, Modifier.fillMaxWidth(), tr("ml", "مل"), decimal = false) },
            confirmButton = { TextButton(onClick = { vm.addWater(ml!!); custom = false }, enabled = ml != null && ml in 10..3000) { Text(tr("Add", "أضف")) } },
            dismissButton = { TextButton(onClick = { custom = false }) { Text(tr("Cancel", "إلغاء")) } },
        )
    }
}

@Composable
private fun GlassButton(ml: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (ml == 0) "+" else "$ml", style = MaterialTheme.typography.titleMedium.tabular(), color = MaterialTheme.colorScheme.primary)
        Text(if (ml == 0) tr("other", "أخرى") else tr("ml", "مل"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A drop that fills from the bottom, with a gentle wave on the surface of the water. */
@Composable
fun WaterDrop(progress: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "level")
    val wave by rememberInfiniteTransition(label = "wave").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase")
    val water = LocalSehaColors.current.water
    val shell = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val drop = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            cubicTo(w * 0.62f, h * 0.3f, w * 0.95f, h * 0.48f, w * 0.95f, h * 0.66f)
            cubicTo(w * 0.95f, h * 0.9f, w * 0.75f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.25f, h * 0.98f, w * 0.05f, h * 0.9f, w * 0.05f, h * 0.66f)
            cubicTo(w * 0.05f, h * 0.48f, w * 0.38f, h * 0.3f, w * 0.5f, h * 0.04f)
            close()
        }
        drawPath(drop, shell)
        clipPath(drop) {
            val top = h * (0.98f - 0.94f * level)
            val surface = Path().apply {
                moveTo(0f, h)
                lineTo(0f, top)
                var x = 0f
                while (x <= w) {
                    lineTo(x, top + sin(wave + x / w * 2 * PI.toFloat()) * h * 0.015f)
                    x += w / 24f
                }
                lineTo(w, h)
                close()
            }
            drawPath(surface, Brush.verticalGradient(listOf(water, water.copy(alpha = 0.75f)), startY = top, endY = h))
            // a highlight on the glass
            drawOval(Color.White.copy(alpha = 0.35f), Offset(w * 0.2f, h * 0.42f), androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.22f))
        }
    }
}
